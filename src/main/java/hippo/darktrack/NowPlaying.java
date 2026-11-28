package hippo.darktrack;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

public class NowPlaying {
    // Looks through all open windows and returns the title of Spotify's main window.
    public static String getSpotifyTitle() {
        final String[] result = {null};

        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (!User32.INSTANCE.IsWindowVisible(hwnd)) return true;

            char[] titleBuffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, titleBuffer, 512);
            String title = Native.toString(titleBuffer);
            if (title.isEmpty()) return true;

            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

            WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                    WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid.getValue());
            if (process == null) return true;

            try {
                char[] pathBuffer = new char[1024];
                IntByReference size = new IntByReference(1024);
                if (Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, pathBuffer, size)) {
                    String exe = Native.toString(pathBuffer).toLowerCase();
                    if (exe.endsWith("spotify.exe")) {
                        result[0] = title;
                        return false; // stop searching
                    }
                }
            } finally {
                Kernel32.INSTANCE.CloseHandle(process);
            }
            return true; // keep searching
        }, null);

        return result[0];
    }
    
     // Returns "Artist - Song" while playing, or null if nothing is playing.
    public static String getCurrentSong() {
        String title = getSpotifyTitle();
        if (title == null) return null;
        if (title.toLowerCase().startsWith("spotify")) return null;
        return title;
    }
    
        // Returns just the song name (the part after "Artist - "), or null if nothing is playing.
    public static String getCurrentSongName() {
        String full = getCurrentSong();
        if (full == null) return null;
        int split = full.indexOf(" - ");
        if (split >= 0) {
            return full.substring(split + 3);
        }
        return full; // no " - " found, so show it as it is
    }
}