package wtf.kity.minecraftxiv;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import wtf.kity.minecraftxiv.mod.Mod;
import wtf.kity.minecraftxiv.network.Capabilities;

import java.util.ArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientInit implements ClientModInitializer {
    public static ClientInit instance;
    public static KeyBinding toggleBinding;
    public static KeyBinding moveCameraBinding;
    public static KeyBinding zoomInBinding;
    public static KeyBinding zoomOutBinding;
    public static Mod mod;
    @Nullable
    public static Capabilities capabilities = null;
    private static final ArrayList<Consumer<Capabilities>> capabilityListeners = new ArrayList<>();

    public static boolean isConnectedToServer() {
        ClientPlayNetworkHandler clientPlayNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
        return clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen();
    }

    public static boolean canChangeCapabilities() {
        MinecraftClient client = MinecraftClient.getInstance();
        // We need to have received capabilities from the server already, and have adequate permissions to change them
        return capabilities != null && client.player != null && client.player.hasPermissionLevel(2);
    }

    public static Capabilities getCapabilities() {
        if (!isConnectedToServer()) {
            return Capabilities.all();
        } else if (capabilities == null) {
            return Capabilities.none();
        }
        return capabilities;
    }

    public static void submitCapabilities(Capabilities capabilities) {
        ClientPlayNetworking.send(capabilities);
    }

    public static void listenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.add(listener);
    }

    public static void unlistenCapabilities(Consumer<Capabilities> listener) {
        capabilityListeners.removeIf(l -> l == listener);
    }

    public static void notifyCapabilityListeners() {
        // Iterate backwards in case it decides to remove itself during execution
        // Rust wouldn't have let me make this mistake >:c
        for (int i = capabilityListeners.size() - 1; i >= 0; i--) {
            capabilityListeners.get(i).accept(getCapabilities());
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        KeyBindingHelper.registerKeyBinding(toggleBinding = new KeyBinding("minecraftxiv.binds.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F4, "minecraftxiv.binds.category"));
        KeyBindingHelper.registerKeyBinding(moveCameraBinding = new KeyBinding("minecraftxiv.binds.moveCamera", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_3, "minecraftxiv.binds.category"));
        KeyBindingHelper.registerKeyBinding(zoomInBinding = new KeyBinding("minecraftxiv.binds.zoomIn", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_6, "minecraftxiv.binds.category"));
        KeyBindingHelper.registerKeyBinding(zoomOutBinding = new KeyBinding("minecraftxiv.binds.zoomOut", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_7, "minecraftxiv.binds.category"));

        mod = new Mod(0, 0, 1.0f, false);

        listenCapabilities(capabilities -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("§7[§5MinecraftXIV§7] §rAllowed features:"));
                player.sendMessage(Text.literal(String.format("%sCamera targeting", capabilities.targetFromCamera() ? "§2" : "§4")));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(Capabilities.ID, (payload, context) -> {
            capabilities = payload;
            notifyCapabilityListeners();
        });
    }
}