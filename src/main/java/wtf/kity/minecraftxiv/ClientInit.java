package wtf.kity.minecraftxiv;


import net.minecraft.client.network.ClientPlayerEntity;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.network.Capabilities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    public static ClientInit instance;
    public static KeyBinding toggleBinding;
    public static KeyBinding moveCameraBinding;
    public static KeyBinding zoomInBinding;
    public static KeyBinding zoomOutBinding;
    public static Mod mod;
    public static Capabilities capabilities = Capabilities.none();

    @SuppressWarnings("resource")
    @Override
    public void onInitializeClient() {
        instance = this;

        KeyBindingHelper.registerKeyBinding(toggleBinding = new KeyBinding("key.minecraftxiv.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(moveCameraBinding = new KeyBinding("key.minecraftxiv.moveCamera", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_3, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(zoomInBinding = new KeyBinding("key.minecraftxiv.zoomIn", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_6, "key.categories.minecraftxiv"));
        KeyBindingHelper.registerKeyBinding(zoomOutBinding = new KeyBinding("key.minecraftxiv.zoomOut", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_7, "key.categories.minecraftxiv"));

        mod = new Mod(0, 0, 1.0f, false);
        PayloadTypeRegistry.playS2C().register(Capabilities.ID, Capabilities.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            capabilities = payload;

            ClientPlayerEntity player = context.client().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[§MinecraftXIV§7] §6Server capabilities:"));
                player.sendMessage(Text.literal(String.format("%sCamera targeting", payload.targetFromCamera() ? "§2" : "§4")));
            }
        });
    }
}