package hippo.darktrack.client;

import hippo.darktrack.MediaKeys;
import hippo.darktrack.NowPlaying;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicScreen extends Screen {
    private final Screen parent;
    private StringWidget songText;
    private int ticks = 0;

    public MusicScreen(Screen parent) {
        super(Component.literal("Music Controls"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0) {
            updateSong();
        }
    }

    private void updateSong() {
        String song = NowPlaying.getCurrentSong();
        Component text = Component.literal(song != null ? song : "Nothing playing");
        int w = this.font.width(text);
        songText.setMessage(text);
        songText.setWidth(w);
        songText.setX(this.width / 2 - w / 2);
    }

    @Override
    protected void init() {
        int h = 20;
        int gap = 6;
        int sideW = 50;
        int midW = 80;
        int total = sideW + midW + sideW + gap * 2;
        int x = this.width / 2 - total / 2;
        int midX = x + sideW + gap;
        int rightX = midX + midW + gap;
        int row1 = this.height / 2 - h - gap / 2;
        int row2 = this.height / 2 + gap / 2;

        addMediaButton("Prev", MediaKeys.PREVIOUS, x, row1, sideW, h);
        addMediaButton("Play/Pause", MediaKeys.PLAY_PAUSE, midX, row1, midW, h);
        addMediaButton("Next", MediaKeys.NEXT, rightX, row1, sideW, h);

        addMediaButton("Vol -", MediaKeys.VOLUME_DOWN, x, row2, sideW, h);
        addMediaButton("Mute", MediaKeys.MUTE, midX, row2, midW, h);
        addMediaButton("Vol +", MediaKeys.VOLUME_UP, rightX, row2, sideW, h);

        this.songText = new StringWidget(0, row1 - 18, 10, 12, Component.empty(), this.font);
        this.addRenderableWidget(this.songText);
        updateSong();
    }

    private void addMediaButton(String label, byte key, int x, int y, int w, int h) {
        this.addRenderableWidget(Button.builder(Component.literal(label),
                b -> MediaKeys.press(key))
                .bounds(x, y, w, h).build());
    }
}