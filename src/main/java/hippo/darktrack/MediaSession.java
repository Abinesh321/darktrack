package hippo.darktrack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MediaSession {
    private static final String SCRIPT = """
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
            function Await($op, $type) {
              $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
              $t.Wait(-1) | Out-Null
              $t.Result
            }
            $mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
            $mgr = Await ($mgrType::RequestAsync()) $mgrType
            $fails = 0
            while ($true) {
              try {
                $out = 'NONE|0|0'
                foreach ($s in $mgr.GetSessions()) {
                  if ($s.SourceAppUserModelId -like '*Spotify*') {
                    $tl = $s.GetTimelineProperties()
                    $pb = $s.GetPlaybackInfo()
                    $extra = 0
                    if ($pb.PlaybackStatus -eq 'Playing') {
                      $d = ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds
                      if ($d -gt 0 -and $d -lt 60000) { $extra = $d }
                    }
                    $pos = [long]($tl.Position.TotalMilliseconds + $extra)
                    $out = '{0}|{1}|{2}' -f $pb.PlaybackStatus, $pos, [long]$tl.EndTime.TotalMilliseconds
                  }
                }
                [Console]::Out.WriteLine($out)
                [Console]::Out.Flush()
                $fails = 0
              } catch {
                $fails++
                if ($fails -ge 5) { exit }
              }
              Start-Sleep -Milliseconds 300
            }
            """;

    private static boolean started = false;
    private static Process process;

    private static String status = "NONE";
    private static long endMs = 0;
    private static long baseMs = 0;     // position at baseAt
    private static long baseAt = 0;     // real clock time when baseMs was set
    private static boolean wasPlaying = false;

    public static synchronized void start() {
        if (started) return;
        started = true;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;

        try {
            Path file = Files.createTempFile("darktrack", ".ps1");
            Files.writeString(file, SCRIPT, StandardCharsets.UTF_8);
            file.toFile().deleteOnExit();

            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass",
                    "-File", file.toString());
            pb.redirectErrorStream(true);
            process = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));

            Thread reader = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleLine(line.trim());
                    }
                } catch (Exception ignored) {
                }
            }, "Darktrack-MediaSession");
            reader.setDaemon(true);
            reader.start();
        } catch (Exception e) {
            System.out.println("[Darktrack] Could not start media session helper: " + e);
        }
    }

    private static synchronized void handleLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 3) return;
        try {
            long pos = Long.parseLong(parts[1]);
            long end = Long.parseLong(parts[2]);
            long now = System.currentTimeMillis();
            boolean playing = parts[0].equals("Playing");

            if (playing && wasPlaying && end == endMs) {
                long predicted = baseMs + (now - baseAt);
                long diff = pos - predicted;
                if (Math.abs(diff) > 3000) {
                    baseMs = pos;                         // a real seek: jump
                } else if (diff > 0) {
                    baseMs = predicted + diff / 4;        // behind: catch up gently
                } else {
                    baseMs = predicted;                   // ahead: never go backwards
                }
            } else {
                baseMs = pos;                             // new song, pause, or resume
            }
            baseAt = now;
            wasPlaying = playing;
            status = parts[0];
            endMs = end;
        } catch (NumberFormatException ignored) {
        }
    }

    public static synchronized long getEndMs() {
        return endMs;
    }

    public static synchronized long getElapsedMs() {
        long ms = baseMs;
        if (wasPlaying) ms += System.currentTimeMillis() - baseAt;
        return Math.max(0, Math.min(ms, endMs));
    }
}