package wtf.kity.minecraftxiv.mod;

import net.minecraft.client.option.Perspective;

public class Mod {

    private float yaw;
    private float pitch;
    private float zoom;
    private boolean enabled;
    private Perspective lastPerspective;

    public Mod(float yaw, float pitch, float zoom, boolean enabled) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.zoom = zoom;
        this.enabled = enabled;
        this.lastPerspective = Perspective.THIRD_PERSON_BACK;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setYawAndPitch(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Perspective getLastPerspective() {
        return lastPerspective;
    }

    public void setLastPerspective(Perspective lastPerspective) {
        this.lastPerspective = lastPerspective;
    }
}
