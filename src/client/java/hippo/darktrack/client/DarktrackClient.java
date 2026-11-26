package hippo.darktrack.client;

import com.mojang.blaze3d.platform.InputConstants;
import hippo.darktrack.MediaKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class DarktrackClient implements ClientModInitializer {
    private static final String ID = "darktrack";

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(ID, "music"));

        KeyMapping prev = register("previous", GLFW.GLFW_KEY_F6, category);
        KeyMapping play = register("play_pause", GLFW.GLFW_KEY_F7, category);
        KeyMapping next = register("next", GLFW.GLFW_KEY_F8, category);
        KeyMapping open = register("open_widget", GLFW.GLFW_KEY_F9, category);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (prev.consumeClick()) MediaKeys.press(MediaKeys.PREVIOUS);
            while (play.consumeClick()) MediaKeys.press(MediaKeys.PLAY_PAUSE);
            while (next.consumeClick()) MediaKeys.press(MediaKeys.NEXT);
            while (open.consumeClick()) client.gui.setScreen(new MusicScreen());
        });
    }

    private static KeyMapping register(String name, int key, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key." + ID + "." + name,
                        InputConstants.Type.KEYSYM, key, category));
    }
}