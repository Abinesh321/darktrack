package hippo.darktrack.client;

import com.mojang.blaze3d.platform.InputConstants;
import hippo.darktrack.MediaKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;

public class DarktrackClient implements ClientModInitializer {
    private static final String ID = "darktrack";

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                Button musicButton = Button.builder(Component.literal("Music"),
                    b -> client.gui.setScreen(new MusicScreen(screen)))
                        .bounds(scaledWidth - 70, 6, 64, 20)
                        .build();
                Screens.getWidgets(screen).add(musicButton);
            }
        });
        
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(ID, "music"));

        KeyMapping prev = register("previous", GLFW.GLFW_KEY_F6, category);
        KeyMapping play = register("play_pause", GLFW.GLFW_KEY_F7, category);
        KeyMapping next = register("next", GLFW.GLFW_KEY_F8, category);
        KeyMapping open = register("open_widget", GLFW.GLFW_KEY_F9, category);
        KeyMapping volDown = register("volume_down", GLFW.GLFW_KEY_LEFT_BRACKET, category);
        KeyMapping volUp = register("volume_up", GLFW.GLFW_KEY_RIGHT_BRACKET, category);
        KeyMapping mute = register("mute", GLFW.GLFW_KEY_BACKSLASH, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (prev.consumeClick()) MediaKeys.press(MediaKeys.PREVIOUS);
            while (play.consumeClick()) MediaKeys.press(MediaKeys.PLAY_PAUSE);
            while (next.consumeClick()) MediaKeys.press(MediaKeys.NEXT);
            while (open.consumeClick()) client.gui.setScreen(new MusicScreen(null));
            while (volDown.consumeClick()) MediaKeys.press(MediaKeys.VOLUME_DOWN);
            while (volUp.consumeClick()) MediaKeys.press(MediaKeys.VOLUME_UP);
            while (mute.consumeClick()) MediaKeys.press(MediaKeys.MUTE);
        });
    }

    private static KeyMapping register(String name, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key." + ID + "." + name,
                        InputConstants.Type.KEYSYM, key, category));
    }
}