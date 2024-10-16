package wtf.kity.minecraftxiv;


import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.network.ModDisallowedPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Client implements ClientModInitializer {

    private static Client instance;
    private KeyBinding toggleBinding;
    private KeyBinding moveCameraBinding;
    private KeyBinding zoomInBinding;
    private KeyBinding zoomOutBinding;
    private Mod mod;
    private boolean forceDisabled = false;

    public static Client getInstance() {
        return instance;
    }

    @SuppressWarnings("resource")
    @Override
    public void onInitializeClient() {
        instance = this;
        KeyBindingHelper.registerKeyBinding(toggleBinding = new KeyBinding("key.minecraftxiv.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(moveCameraBinding = new KeyBinding("key.minecraftxiv.moveCamera", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_3, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(zoomInBinding = new KeyBinding("key.minecraftxiv.zoomIn", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_6, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(zoomOutBinding = new KeyBinding("key.minecraftxiv.zoomOut", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_7, "key.categories.minecraftxiv"));

        this.mod = new Mod(0, 0, 1.0f, false);
        /*
        ClientPlayNetworking.registerGlobalReceiver(new Identifier("minecraftxiv", "is_disallowed"), (client, handler, buf, responseSender) -> {
            this.forceDisabled = true;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§7[§MinecraftXIV§7] §6Oh! This server disabled the use of this mod. This means you can't use this feature on this server!"));
            }
        });
         */
        PayloadTypeRegistry.playS2C().register(ModDisallowedPayload.ID, ModDisallowedPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(ModDisallowedPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            this.forceDisabled = true;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§7[§MinecraftXIV§7] §6Oh! This server disabled the use of this mod. This means you can't use this feature on this server!"));
            }
        });
    }

    public KeyBinding getToggleBinding() {
        return toggleBinding;
    }
    public KeyBinding getMoveCameraBinding() {
        return moveCameraBinding;
    }
    public KeyBinding getZoomInBinding() { return zoomInBinding; }
    public KeyBinding getZoomOutBinding() { return zoomOutBinding; }

    public Mod getMod() {
        return mod;
    }

    public boolean isForceDisabled() {
        return forceDisabled;
    }

    public void setForceDisabled(boolean forceDisabled) {
        this.forceDisabled = forceDisabled;
    }
}