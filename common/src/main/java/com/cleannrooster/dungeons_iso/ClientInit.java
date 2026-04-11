package com.cleannrooster.dungeons_iso;


import com.cleannrooster.dungeons_iso.config.Config;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.lwjgl.glfw.GLFW;

public class ClientInit {
    public static ClientInit instance;

    public static KeyBinding toggleBinding;
    public static KeyBinding isoBinding;
    public static KeyBinding verticalBinding;
    public static KeyBinding lockOn;
    public static KeyBinding clickToMove;
    public static KeyBinding moveCameraBinding;
    public static KeyBinding zoomInBinding;
    public static KeyBinding zoomOutBinding;
    public static KeyBinding rotateToggle;
    public static KeyBinding cycleTargetBinding;
    public static KeyBinding openLootMenu;
    public static KeyBinding rotateCounterClockwise;
    public static KeyBinding rotateClockwise;
    public static KeyBinding interact;
    public static KeyBinding contextToggleBinding;

    /**
     * Creates all KeyBinding objects.
     * Called during NeoForge's RegisterKeyMappingsEvent and from Fabric's onInitializeClient.
     * Does NOT call any platform-specific registration API — see getAllKeyBindings().
     */
    public static void registerKeyBindings() {
        toggleBinding = new KeyBinding(
                "dungeons_iso.binds.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "dungeons_iso.binds.category"
        );
        isoBinding = new KeyBinding(
                "dungeons_iso.binds.iso",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_HOME,
                "dungeons_iso.binds.category"
        );
        moveCameraBinding = new KeyBinding(
                "dungeons_iso.binds.moveCamera",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_3,
                "dungeons_iso.binds.category"
        );
        lockOn = new KeyBinding(
                "dungeons_iso.binds.lockOn",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "dungeons_iso.binds.category"
        );
        verticalBinding = new KeyBinding(
                "dungeons_iso.binds.verticalBinding",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "dungeons_iso.binds.category"
        );
        clickToMove = new KeyBinding(
                "dungeons_iso.binds.clickToMove",
                InputUtil.Type.MOUSE,
                InputUtil.UNKNOWN_KEY.getCode(),
                "dungeons_iso.binds.category"
        );
        zoomInBinding = new KeyBinding(
                "dungeons_iso.binds.zoomIn",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_KEY_UP,
                "dungeons_iso.binds.category"
        );
        zoomOutBinding = new KeyBinding(
                "dungeons_iso.binds.zoomOut",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_KEY_DOWN,
                "dungeons_iso.binds.category"
        );
        rotateToggle = new KeyBinding(
                "dungeons_iso.binds.rotateToggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "dungeons_iso.binds.category"
        );
        rotateClockwise = new KeyBinding(
                "dungeons_iso.binds.rotateClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "dungeons_iso.binds.category"
        );
        interact = new KeyBinding(
                "dungeons_iso.binds.interact",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "dungeons_iso.binds.category"
        );
        rotateCounterClockwise = new KeyBinding(
                "dungeons_iso.binds.rotateCounterClockwise",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "dungeons_iso.binds.category"
        );
        contextToggleBinding = new KeyBinding(
                "dungeons_iso.binds.dynCameraToggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_END,
                "dungeons_iso.binds.category"
        );
        cycleTargetBinding = new KeyBinding(
                "dungeons_iso.binds.cycleTargetBinding",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                "dungeons_iso.binds.category"
        );
        openLootMenu = new KeyBinding(
                "dungeons_iso.binds.openLootMenu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "dungeons_iso.binds.category"
        );
    }

    /**
     * Returns all key bindings so platform modules can register them.
     * Fabric: KeyBindingHelper.registerKeyBinding(). NeoForge: event.register().
     */
    public static KeyBinding[] getAllKeyBindings() {
        return new KeyBinding[]{
                toggleBinding, isoBinding, moveCameraBinding, lockOn, verticalBinding,
                clickToMove, zoomInBinding, zoomOutBinding, rotateToggle, rotateClockwise,
                interact, rotateCounterClockwise, contextToggleBinding, cycleTargetBinding,
                openLootMenu
        };
    }

    /**
     * Called during client setup (after key bindings are registered).
     */
    public static void init() {
        instance = new ClientInit();
        Config.GSON.load();
        com.cleannrooster.dungeons_iso.config.FirstTimeState.load();
    }

    private static void drawCuboidShapeOutline(MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape shape, double offsetX, double offsetY, double offsetZ, float red, float green, float blue, float alpha) {
        MatrixStack.Entry entry = matrices.peek();
        shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float)(maxX - minX);
            float l = (float)(maxY - minY);
            float m = (float)(maxZ - minZ);
            float n = MathHelper.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            vertexConsumer.vertex(entry, (float)(minX + offsetX), (float)(minY + offsetY), (float)(minZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
            vertexConsumer.vertex(entry, (float)(maxX + offsetX), (float)(maxY + offsetY), (float)(maxZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
        });
    }
}
