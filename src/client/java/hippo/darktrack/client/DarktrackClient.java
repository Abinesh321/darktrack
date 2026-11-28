package hippo.darktrack.client;

import com.mojang.blaze3d.platform.InputConstants;
import hippo.darktrack.MediaKeys;
import hippo.darktrack.MediaSession;
import hippo.darktrack.NowPlaying;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.AbstractWidget;

public class DarktrackClient implements ClientModInitializer {
    private static final String ID = "darktrack";

    @Override
    public void onInitializeClient() {
        MediaSession.start();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
             if (screen instanceof PauseScreen) {
                 SpriteIconButton musicButton = SpriteIconButton.builder(Component.literal("Music"),
                     b -> client.gui.setScreen(new PlayerScreen(screen)), true)
                .size(20, 20)
                .sprite(Identifier.fromNamespaceAndPath(ID, "music_icon"), 16, 16)
                .build();

                // look for Mod Menu's "Mods" button on this screen
                String modsText = Component.translatable("modmenu.title").getString();
                AbstractWidget modMenuButton = null;
                for (AbstractWidget w : Screens.getWidgets(screen)) {
                    if (w.getMessage().getString().startsWith(modsText)) {
                        modMenuButton = w;
                        break;
                    }
                }
                if (modMenuButton != null) {
                    // right next to Mod Menu's button, vertically centered with it
                    int x = modMenuButton.getX() + modMenuButton.getWidth() + 4;
                    musicButton.setX(Math.min(x, scaledWidth - 24));
                    musicButton.setY(modMenuButton.getY() + (modMenuButton.getHeight() - 20) / 2);
                } else {
                    // no Mod Menu: stay in the top-right corner
                    musicButton.setX(scaledWidth - 26);
                    musicButton.setY(6);
                } 

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
        KeyMapping toggleHud = register("toggle_hud", GLFW.GLFW_KEY_F10, category);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(ID, "song_hud"), SongHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            
            while (prev.consumeClick()) { MediaKeys.press(MediaKeys.PREVIOUS); SongHud.flash(0); }
            while (play.consumeClick()) { MediaKeys.press(MediaKeys.PLAY_PAUSE); SongHud.flash(1); }
            while (next.consumeClick()) { MediaKeys.press(MediaKeys.NEXT); SongHud.flash(2); }
            while (open.consumeClick()) client.gui.setScreen(new PlayerScreen(null));
            while (volDown.consumeClick()) MediaKeys.press(MediaKeys.VOLUME_DOWN);
            while (volUp.consumeClick()) MediaKeys.press(MediaKeys.VOLUME_UP);
            while (mute.consumeClick()) MediaKeys.press(MediaKeys.MUTE);
            while (toggleHud.consumeClick()) SongHud.enabled = !SongHud.enabled;
            SongHud.tick();
        });
    }

    private static KeyMapping register(String name, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key." + ID + "." + name,
                        InputConstants.Type.KEYSYM, key, category));
    }
}