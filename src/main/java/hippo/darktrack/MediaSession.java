package hippo.darktrack;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MediaSession {
    private static final String BOILERPLATE = """
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
            function Await($op, $type) {
              $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
              $t.Wait(-1) | Out-Null
              $t.Result
            }
            $mgrType = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
            $mgr = Await ($mgrType::RequestAsync()) $mgrType
            """;

    private static final String READ_SCRIPT = BOILERPLATE + """
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

    private static final String SEEK_SCRIPT = "param([long]$ms)\n" + BOILERPLATE + """
            foreach ($s in $mgr.GetSessions()) {
              if ($s.SourceAppUserModelId -like '*Spotify*') {
                Await ($s.TryChangePlaybackPositionAsync([long]($ms * 10000))) ([bool]) | Out-Null
              }
            }
            """;

    private static boolean started = false;
    private static Process process;
    private static Path seekScriptFile;

    private static String status = "NONE";
    private static long endMs = 0;
    private static long baseMs = 0;     // position at baseAt
    private static long baseAt = 0;     // real clock time when baseMs was set
    private static boolean wasPlaying = false;
    private static long suppressUntil = 0;   // ignore Windows' numbers until this time (after a seek)

    public static synchronized void start() {
        if (started) return;
        started = true;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;

        try {
            Path file = Files.createTempFile("darktrack", ".ps1");
            Files.writeString(file, READ_SCRIPT, StandardCharsets.UTF_8);
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
        if (System.currentTimeMillis() < suppressUntil) return;

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

    // Jumps the song to a position (in milliseconds).
    public static void seekTo(long ms) {
        final long target;
        synchronized (MediaSession.class) {
            target = Math.max(0, Math.min(ms, endMs));
            long now = System.currentTimeMillis();
            baseMs = target;                 // move our own track right away
            baseAt = now;
            suppressUntil = now + 2500;      // ignore Windows' old numbers for a moment
        }

        Thread t = new Thread(() -> {
            try {
                Path script;
                synchronized (MediaSession.class) {
                    if (seekScriptFile == null) {
                        seekScriptFile = Files.createTempFile("darktrack-seek", ".ps1");
                        Files.writeString(seekScriptFile, SEEK_SCRIPT, StandardCharsets.UTF_8);
                        seekScriptFile.toFile().deleteOnExit();
                    }
                    script = seekScriptFile;
                }
                new ProcessBuilder("powershell.exe", "-NoProfile", "-WindowStyle", "Hidden",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                        "-ms", String.valueOf(target))
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
            } catch (Exception e) {
                System.out.println("[Darktrack] Seek failed: " + e);
            }
        }, "Darktrack-Seek");
        t.setDaemon(true);
        t.start();
    }
}