package com.cleannrooster.dungeons_iso.util;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Reads the left analog stick of the first connected controller directly through GLFW (no
 * MidnightControls / controller-mod dependency). Must be polled from the main thread — call it
 * from the client input tick.
 */
public final class JoystickInput {

    /** Radial deadzone below which the stick is treated as centered. */
    private static final float DEADZONE = 0.18f;

    private JoystickInput() {
    }

    /**
     * Polls the first present controller's left stick and returns its deflection in Minecraft
     * key-input convention: {@code x} = forward (W positive), {@code y} = sideways (left
     * positive) — i.e. the axes vanilla's KeyboardInput writes to movementForward /
     * movementSideways, so the result flows through the existing camera-relative rotation
     * unchanged. Applies a radial deadzone with edge-rescaling for a smooth ramp.
     *
     * @return the deflection, or {@code null} when no controller is connected or the stick is
     *         inside the deadzone.
     */
    public static Vector2f readLeftStick() {
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
            if (!GLFW.glfwJoystickPresent(jid)) {
                continue;
            }
            float x;
            float y;
            if (GLFW.glfwJoystickIsGamepad(jid)) {
                // Mapped gamepad: use the SDL gamepad mapping so LEFT_X/LEFT_Y are consistent
                // across controllers regardless of raw axis order.
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    GLFWGamepadState state = GLFWGamepadState.malloc(stack);
                    if (!GLFW.glfwGetGamepadState(jid, state)) {
                        continue;
                    }
                    x = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_X);
                    y = state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y);
                }
            } else {
                // Unmapped joystick: fall back to the first two raw axes.
                FloatBuffer axes = GLFW.glfwGetJoystickAxes(jid);
                if (axes == null || axes.remaining() < 2) {
                    continue;
                }
                x = axes.get(0);
                y = axes.get(1);
            }
            return applyDeadzone(x, y);
        }
        return null;
    }

    private static Vector2f applyDeadzone(float x, float y) {
        float mag = (float) Math.sqrt(x * x + y * y);
        if (mag < DEADZONE) {
            return null;
        }
        // Ramp smoothly from the deadzone edge (mag == DEADZONE -> 0) to the rim (mag >= 1 -> 1),
        // preserving stick direction.
        float scaled = Math.min((mag - DEADZONE) / (1.0f - DEADZONE), 1.0f);
        float nx = x / mag * scaled;
        float ny = y / mag * scaled;
        // GLFW axes: +X right, +Y down. Key convention: +forward = up = -Y, +sideways(left) = -X.
        return new Vector2f(-ny, -nx);
    }
}
