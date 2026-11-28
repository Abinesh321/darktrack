package hippo.darktrack.client;

import hippo.darktrack.MediaSession;
import hippo.darktrack.NowPlaying;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SongHud {
    public static boolean enabled = true;

    private static int state = 0;      // 0 = Spotify not found, 1 = playing, 2 = paused
    private static String name = "";
    private static int ticks = 0;

    // size of the time text: 1.0 = normal, smaller = tinier
    private static final float TIME_SCALE = 0.75f;

    private static final long[] flashAt = new long[3];   // 0 = prev, 1 = play/pause, 2 = next

    // called when a media key is pressed, so its icon lights up
    public static void flash(int button) {
        flashAt[button] = System.currentTimeMillis();
    }

    private static int iconColor(int button) {
        return System.currentTimeMillis() - flashAt[button] < 250 ? 0xFFFFFF55 : 0xFFAAAAAA;
    }

    // Called every game tick; checks Spotify once per second.
    public static void tick() {
        if (!enabled) return;
        if (ticks++ % 20 != 0) return;

        String title = NowPlaying.getSpotifyTitle();
        if (title == null) {
            state = 0;
            name = "";
        } else if (title.toLowerCase().startsWith("spotify")) {
            state = 2;
        } else {
            state = 1;
            int split = title.indexOf(" - ");
            name = split >= 0 ? title.substring(split + 3) : title;
        }
    }

    // Called every frame; only draws.
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!enabled || state == 0) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // keeps the last song name while paused
        String label = fit(font, name.isEmpty() ? "Spotify" : name, 140);
        long endMs = MediaSession.getEndMs();
        boolean showBar = endMs > 0;

        int boxW = Math.max(font.width(label) + 26, 100);
        int boxH = showBar ? 41 : 29;
        int x = mc.getWindow().getGuiScaledWidth() - boxW - 6;
        int y = 6;

        graphics.fill(x, y, x + boxW, y + boxH, 0x90000000);

        // music note + song name (row 1)
        drawNote(graphics, x + 5, y + 3, state == 1 ? 0xFF55FF55 : 0xFFFFAA00);
        graphics.text(font, label, x + 19, y + 4, 0xFFFFFFFF, true);

        // control icons (row 2), centered
        int cx = x + boxW / 2;
        int cy = y + 22;
        drawPrev(graphics, cx - 13, cy, iconColor(0));
        drawPlayPause(graphics, cx, cy, iconColor(1), state == 1);
        drawNext(graphics, cx + 13, cy, iconColor(2));

        // times and track (row 3)
        if (showBar) {
            long elapsed = MediaSession.getElapsedMs();
            String left = formatTime(elapsed);
            String right = formatTime(endMs);

            int rowY = y + 29;
            int leftW = (int) Math.ceil(font.width(left) * TIME_SCALE);
            int rightW = (int) Math.ceil(font.width(right) * TIME_SCALE);
            int barX = x + 5 + leftW + 4;
            int barEnd = x + boxW - 5 - rightW - 4;
            int barW = Math.max(10, barEnd - barX);

            smallText(graphics, font, left, x + 5, rowY + 1, 0xFFAAAAAA);
            smallText(graphics, font, right, x + boxW - 5 - rightW, rowY + 1, 0xFFAAAAAA);

            graphics.fill(barX, rowY + 4, barX + barW, rowY + 5, 0xFF555555);
            int filled = (int) (barW * Math.min(1.0, (double) elapsed / endMs));
            graphics.fill(barX, rowY + 4, barX + filled, rowY + 5, state == 1 ? 0xFF55FF55 : 0xFFFFAA00);
        }
    }

    // small music note, about 9 wide and 10 tall
    private static void drawNote(GuiGraphicsExtractor g, int ix, int iy, int color) {
        g.fill(ix + 1, iy + 7, ix + 4, iy + 8, color);    // head
        g.fill(ix, iy + 8, ix + 5, iy + 9, color);
        g.fill(ix + 1, iy + 9, ix + 4, iy + 10, color);
        g.fill(ix + 4, iy, ix + 5, iy + 8, color);        // stem
        g.fill(ix + 5, iy, ix + 8, iy + 1, color);        // flag
        g.fill(ix + 7, iy + 1, ix + 9, iy + 3, color);
        g.fill(ix + 6, iy + 3, ix + 8, iy + 4, color);
    }

    // |<
    private static void drawPrev(GuiGraphicsExtractor g, int cx, int cy, int color) {
        int x0 = cx - 3;
        g.fill(x0, cy - 3, x0 + 1, cy + 4, color);
        for (int k = 0; k < 4; k++) {
            g.fill(x0 + 1 + k, cy - 3 + (3 - k), x0 + 2 + k, cy + 4 - (3 - k), color);
        }
    }

    // >|
    private static void drawNext(GuiGraphicsExtractor g, int cx, int cy, int color) {
        int x0 = cx - 2;
        for (int k = 0; k < 4; k++) {
            g.fill(x0 + k, cy - 3 + k, x0 + k + 1, cy + 4 - k, color);
        }
        g.fill(x0 + 4, cy - 3, x0 + 5, cy + 4, color);
    }

    // pause bars while playing, play triangle while paused
    private static void drawPlayPause(GuiGraphicsExtractor g, int cx, int cy, int color, boolean playing) {
        if (playing) {
            g.fill(cx - 3, cy - 3, cx - 1, cy + 4, color);
            g.fill(cx + 1, cy - 3, cx + 3, cy + 4, color);
        } else {
            int x0 = cx - 2;
            for (int k = 0; k < 4; k++) {
                g.fill(x0 + k, cy - 3 + k, x0 + k + 1, cy + 4 - k, color);
            }
        }
    }

    // draws text scaled down, with (x, y) as its top-left corner
    private static void smallText(GuiGraphicsExtractor graphics, Font font,
                                  String text, int x, int y, int color) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(TIME_SCALE, TIME_SCALE);
        graphics.text(font, text, 0, 0, color, true);
        pose.popMatrix();
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + "...") > maxWidth) {
            end--;
        }
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "...";
    }
}