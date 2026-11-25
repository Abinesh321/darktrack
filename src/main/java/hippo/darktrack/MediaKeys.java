package hippo.darktrack;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class MediaKeys {
    private interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);
        void keybd_event(byte vk, byte scan, int flags, long extra);
    }

    private static final int KEYUP = 0x2;
    public static final byte NEXT = (byte) 0xB0;
    public static final byte PREVIOUS = (byte) 0xB1;
    public static final byte PLAY_PAUSE = (byte) 0xB3;

    public static void press(byte key) {
        User32.INSTANCE.keybd_event(key, (byte) 0, 0, 0);
        User32.INSTANCE.keybd_event(key, (byte) 0, KEYUP, 0);
    }
}