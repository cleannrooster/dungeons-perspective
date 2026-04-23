package com.cleannrooster.dungeons_iso;


import com.cleannrooster.dungeons_iso.config.Config;
import com.cleannrooster.dungeons_iso.config.FirstTimeState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lwjgl.glfw.GLFW;

public class ClientInit {
    public static ClientInit instance;

    public static KeyMapping toggleBinding;
    public static KeyMapping isoBinding;
    public static KeyMapping verticalBinding;
    public static KeyMapping lockOn;
    public static KeyMapping clickToMove;
    public static KeyMapping moveCameraBinding;
    public static KeyMapping zoomInBinding;
    public static KeyMapping zoomOutBinding;
    public static KeyMapping rotateToggle;
    public static KeyMapping cycleTargetBinding;
    public static KeyMapping openLootMenu;
    public static KeyMapping rotateCounterClockwise;
    public static KeyMapping rotateClockwise;
    public static KeyMapping interact;
    public static KeyMapping contextToggleBinding;

    /**
     * Creates all KeyMapping objects.
     * Called during NeoForge's RegisterKeyMappingsEvent and from Fabric's onInitializeClient.
     * Does NOT call any platform-specific registration API — see getAllKeyBindings().
     */
    public static void registerKeyBindings() {
        toggleBinding = new KeyMapping(
                "dungeons_iso.binds.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4,
                "dungeons_iso.binds.category"
        );
        isoBinding = new KeyMapping(
                "dungeons_iso.binds.iso",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_HOME,
                "dungeons_iso.binds.category"
        );
        moveCameraBinding = new KeyMapping(
                "dungeons_iso.binds.moveCamera",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_3,
                "dungeons_iso.binds.category"
        );
        lockOn = new KeyMapping(
                "dungeons_iso.binds.lockOn",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "dungeons_iso.binds.category"
        );
        verticalBinding = new KeyMapping(
                "dungeons_iso.binds.verticalBinding",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "dungeons_iso.binds.category"
        );
        clickToMove = new KeyMapping(
                "dungeons_iso.binds.clickToMove",
                InputConstants.Type.MOUSE,
                InputConstants.UNKNOWN_KEY.getCode(),
                "dungeons_iso.binds.category"
        );
        zoomInBinding = new KeyMapping(
                "dungeons_iso.binds.zoomIn",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_KEY_UP,
                "dungeons_iso.binds.category"
        );
        zoomOutBinding = new KeyMapping(
                "dungeons_iso.binds.zoomOut",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_KEY_DOWN,
                "dungeons_iso.binds.category"
        );
        rotateToggle = new KeyMapping(
                "dungeons_iso.binds.rotateToggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DELETE,
                "dungeons_iso.binds.category"
        );
        rotateClockwise = new KeyMapping(
                "dungeons_iso.binds.rotateClockwise",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                "dungeons_iso.binds.category"
        );
        interact = new KeyMapping(
                "dungeons_iso.binds.interact",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "dungeons_iso.binds.category"
        );
        rotateCounterClockwise = new KeyMapping(
                "dungeons_iso.binds.rotateCounterClockwise",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                "dungeons_iso.binds.category"
        );
        contextToggleBinding = new KeyMapping(
                "dungeons_iso.binds.dynCameraToggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_END,
                "dungeons_iso.binds.category"
        );
        cycleTargetBinding = new KeyMapping(
                "dungeons_iso.binds.cycleTargetBinding",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                "dungeons_iso.binds.category"
        );
        openLootMenu = new KeyMapping(
                "dungeons_iso.binds.openLootMenu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "dungeons_iso.binds.category"
        );
    }

    /**
     * Returns all key bindings so platform modules can register them.
     * Fabric: KeyBindingHelper.registerKeyBinding(). NeoForge: event.register().
     */
    public static KeyMapping[] getAllKeyBindings() {
        return new KeyMapping[]{
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
        FirstTimeState.load();
    }

    private static void drawCuboidShapeOutline(PoseStack matrices, VertexConsumer vertexConsumer, VoxelShape shape, double offsetX, double offsetY, double offsetZ, float red, float green, float blue, float alpha) {
        PoseStack.Entry entry = matrices.peek();
        shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float)(maxX - minX);
            float l = (float)(maxY - minY);
            float m = (float)(maxZ - minZ);
            float n = Mth.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            vertexConsumer.vertex(entry, (float)(minX + offsetX), (float)(minY + offsetY), (float)(minZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
            vertexConsumer.vertex(entry, (float)(maxX + offsetX), (float)(maxY + offsetY), (float)(maxZ + offsetZ)).color(red, green, blue, alpha).normal(entry, k, l, m);
        });
    }
}
