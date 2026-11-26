package hippo.darktrack.client;

import hippo.darktrack.MediaKeys;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicScreen extends Screen {
    public MusicScreen() {
        super(Component.literal("Music Controls"));
    }

    @Override
    protected void init() {
        int h = 20;
        int gap = 6;
        int prevW = 50;
        int playW = 80;
        int nextW = 50;
        int total = prevW + playW + nextW + gap * 2;
        int x = this.width / 2 - total / 2;
        int y = this.height / 2 - h / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Prev"),
                b -> MediaKeys.press(MediaKeys.PREVIOUS))
                .bounds(x, y, prevW, h).build());

        this.addRenderableWidget(Button.builder(Component.literal("Play/Pause"),
                b -> MediaKeys.press(MediaKeys.PLAY_PAUSE))
                .bounds(x + prevW + gap, y, playW, h).build());

        this.addRenderableWidget(Button.builder(Component.literal("Next"),
                b -> MediaKeys.press(MediaKeys.NEXT))
                .bounds(x + prevW + gap + playW + gap, y, nextW, h).build());
    }
}