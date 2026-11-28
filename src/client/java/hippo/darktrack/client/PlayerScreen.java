package hippo.darktrack.client;

import hippo.darktrack.MediaKeys;
import hippo.darktrack.MediaSession;
import hippo.darktrack.NowPlaying;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PlayerScreen extends Screen {
    private static final int CARD_W = 240;
    private static final int CARD_H = 138;

    private final Screen parent;
    private SeekSlider slider;

    private int cardX, cardY, sliderX, sliderW;
    private String songName = "";
    private int ticks = 0;

    private boolean pendingSeek = false;
    private long lastUserChange = 0;
    private long lastSeekAt = 0;

    public PlayerScreen(Screen parent) {
        super(Component.literal("Now Playing"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    protected void init() {
        cardX = this.width / 2 - CARD_W / 2;
        cardY = this.height / 2 - CARD_H / 2;
        sliderX = cardX + 12;
        sliderW = CARD_W - 24;

        // progress slider (drag or click to seek)
        slider = new SeekSlider(sliderX, cardY + 40, sliderW, 12, () -> {
            lastUserChange = System.currentTimeMillis();
            pendingSeek = true;
        });
        this.addRenderableWidget(slider);

        int gap = 6;
        int sideW = 60;
        int midW = 80;
        int total = sideW + midW + sideW + gap * 2;
        int x = this.width / 2 - total / 2;
        int midX = x + sideW + gap;
        int rightX = midX + midW + gap;

        int row1 = cardY + 74;
        int row2 = cardY + 104;

        addMediaButton("Prev", MediaKeys.PREVIOUS, x, row1, sideW, 24);
        addMediaButton("Play/Pause", MediaKeys.PLAY_PAUSE, midX, row1, midW, 24);
        addMediaButton("Next", MediaKeys.NEXT, rightX, row1, sideW, 24);

        addMediaButton("Vol -", MediaKeys.VOLUME_DOWN, x, row2, sideW, 20);
        addMediaButton("Mute", MediaKeys.MUTE, midX, row2, midW, 20);
        addMediaButton("Vol +", MediaKeys.VOLUME_UP, rightX, row2, sideW, 20);

        updateSongName();
    }

    private void addMediaButton(String label, byte key, int x, int y, int w, int h) {
        this.addRenderableWidget(Button.builder(Component.literal(label),
                b -> MediaKeys.press(key))
                .bounds(x, y, w, h).build());
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0) updateSongName();

        long end = MediaSession.getEndMs();
        long now = System.currentTimeMillis();
        slider.active = end > 0;

        if (pendingSeek && now - lastUserChange > 400 && end > 0) {
            // the user stopped moving the slider: do one seek
            MediaSession.seekTo((long) (slider.getProgress() * end));
            pendingSeek = false;
            lastSeekAt = now;
        } else if (!pendingSeek && now - lastSeekAt > 1500 && end > 0) {
            // otherwise follow the song
            slider.setProgress((double) MediaSession.getElapsedMs() / end);
        }
    }

    private void updateSongName() {
        String title = NowPlaying.getSpotifyTitle();
        if (title == null) {
            songName = "";
        } else if (!title.toLowerCase().startsWith("spotify")) {
            int split = title.indexOf(" - ");
            songName = split >= 0 ? title.substring(split + 3) : title;
        }
        // if paused ("Spotify ..."), keep the last name
    }

    // Draws the background and all the text; the base class then draws the buttons on top.
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);   // dims the game behind

        // card with a border
        graphics.fill(cardX - 1, cardY - 1, cardX + CARD_W + 1, cardY + CARD_H + 1, 0xFF55FF55);
        graphics.fill(cardX, cardY, cardX + CARD_W, cardY + CARD_H, 0xFF14141E);

        Font font = this.font;

        // title
        String title = "Now Playing";
        graphics.text(font, title, cardX + (CARD_W - font.width(title)) / 2, cardY + 8, 0xFF8888AA, true);

        // song name, larger
        float scale = 1.25f;
        String shown = fit(font, songName.isEmpty() ? "Nothing playing" : songName,
                (int) ((CARD_W - 24) / scale));
        int nameW = (int) (font.width(shown) * scale);
        scaledText(graphics, font, shown, cardX + (CARD_W - nameW) / 2, cardY + 21, scale, 0xFFFFFFFF);

        // times under the slider (shows the target time while dragging)
        long end = MediaSession.getEndMs();
        long shownMs = pendingSeek ? (long) (slider.getProgress() * end) : MediaSession.getElapsedMs();
        String left = formatTime(shownMs);
        String right = formatTime(end);
        graphics.text(font, left, sliderX, cardY + 56, 0xFFAAAAAA, true);
        graphics.text(font, right, sliderX + sliderW - font.width(right), cardY + 56, 0xFFAAAAAA, true);
    }

    private static void scaledText(GuiGraphicsExtractor graphics, Font font, String text,
                                   int x, int y, float scale, int color) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
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

    // A slider that has no text and tells us when the user changes it.
    private static class SeekSlider extends AbstractSliderButton {
        private final Runnable onUserChange;

        SeekSlider(int x, int y, int w, int h, Runnable onUserChange) {
            super(x, y, w, h, Component.empty(), 0.0);
            this.onUserChange = onUserChange;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.empty());
        }

        @Override
        protected void applyValue() {
            if (onUserChange != null) onUserChange.run();
        }

        double getProgress() {
            return this.value;
        }

        void setProgress(double p) {
            this.value = Math.max(0.0, Math.min(1.0, p));
        }
    }
}