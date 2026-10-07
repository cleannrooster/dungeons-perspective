package com.cleannrooster.dungeons_iso.config;

import dev.isxander.yacl3.config.v2.api.SerialEntry;

public class Config {
    /**
     * YACL's handler when YACL is installed, plain Gson over the same file when it is not.
     * {@code instance()} / {@code load()} / {@code save()} behave identically either way.
     *
     * <p>The {@code @SerialEntry} annotations below stay regardless: YACL reads them when it is
     * present, and when it is absent the JVM drops annotations whose class cannot be resolved, so
     * they cost nothing rather than throwing.
     */
    public static final ConfigHolder<Config> GSON = ConfigHolder.create(new Config());
    @SerialEntry
    public boolean XIV =  false;

    /**
     * "Controller mode" master toggle. When on, it forces a controller/one-handed-friendly preset,
     * overriding the individual toggles below at their behavior read sites (the stored values are
     * left untouched so turning Controller mode back off restores them):
     * turn-to-mouse OFF, contextual targeting ON, movement-based targeting ON, contextual block
     * interaction ON, click-to-move OFF, roll-towards-cursor (Combat Roll compat) OFF.
     */
    @SerialEntry
    public boolean controllerMode =  false;

    @SerialEntry
    public boolean onStartup =  true;
    @SerialEntry
    public boolean force =  false;
    @SerialEntry
    public boolean fogOfWar =  false;
    @SerialEntry
    public boolean distanceFog =  true;
    @SerialEntry
    public boolean renderDistanceCap =  true;
    @SerialEntry
    public float cullAngle = 6.0F;
    @SerialEntry
    public float coneHalfAngle = 45.0F;
    @SerialEntry
    public boolean backCull = false;

    // ---------------------------------------------------------------- room / roof culling
    // Detects the enclosed space the player is standing in and removes its ceiling, instead of
    // boring a cylinder through whatever happens to sit between the player and the camera.
    // Scanning runs on a worker thread; the cost that reaches the frame is chunk re-meshing,
    // which roomSectionsPerTick throttles. Not yet exposed in the YACL screen — edit
    // dungeons_iso_v5.json directly.

    /**
     * Write a line of culling state to the log once a second. Diagnostic only, and off by default:
     * it never falls silent on its own, since the scan counter advances every second. The keybind
     * report is unaffected and always available.
     */
    @SerialEntry
    public boolean cullDebugLog = false;

    /**
     * Disable Sodium's occlusion culling while blocks are being removed.
     *
     * <p>Off by default, and worth leaving off: with it on, Sodium draws every section in the
     * frustum with no visibility graph, which in a large pack costs far more than the culling
     * itself. It was needed when the cylinder culler removed blocks without reporting which
     * sections it had changed, leaving Sodium's visibility data stale; the scanners now rebuild
     * exactly the sections they affect, which regenerates it.
     *
     * <p>Turn on only if you see missing terrain through a culled opening.
     */
    @SerialEntry
    public boolean disableOcclusionCulling = false;

    /** Master toggle for room/roof culling. When off, the cylinder culler behaves as before. */
    @SerialEntry
    public boolean roomCulling = true;
    /** Maximum horizontal reach of the flood, in blocks. The walls should stop it well before this. */
    @SerialEntry
    public int roomRadius = 48;
    /** How far the room footprint grows outward to swallow the surrounding walls. */
    @SerialEntry
    public int roomWallThickness = 2;
    /**
     * How far below the player's own ceiling height the flood may still go, in blocks. This is what
     * keeps the room to the room: a doorway's lintel sits well below the ceiling, so refusing to
     * duck under a lower ceiling stops the flood at the threshold instead of spilling into the next
     * room and taking its roof off too. Raise it for rooms with stepped or vaulted ceilings; lower
     * it if adjacent rooms are still being included.
     */
    @SerialEntry
    public int roomCeilingTolerance = 2;
    /**
     * How far above the room's ceiling to keep removing, in blocks.
     *
     * <p>This is what lets you see into the ground floor of a building: the walk crosses air gaps
     * rather than stopping at the first solid run, so an upper storey's floor, its ceiling and the
     * roof above all come away together instead of the second floor being left sitting on top of a
     * removed first-floor ceiling.
     *
     * <p>Only solid blocks in that span cost anything — the air between storeys is free. Lower it
     * if a room buried under deep terrain removes more of the hillside than you want.
     */
    @SerialEntry
    public int roomCoverHeight = 24;
    /** Cells visited before the space is judged to be a cave system rather than a room. */
    @SerialEntry
    public int roomMaxVolume = 200000;
    /** Sections re-meshed per client tick. The one knob that directly trades latency for frame time. */
    @SerialEntry
    public int roomSectionsPerTick = 4;
    /** Flood cells per worker slice before it yields. Lower spreads a scan over more ticks. */
    @SerialEntry
    public int roomNodesPerSlice = 3000;
    /** Minimum ticks between scans, on top of requiring the player to have changed block. */
    @SerialEntry
    public int roomRescanCooldown = 4;

    // ---------------------------------------------------------------- shape culling
    // Replaces the cylinder between the camera and the player. Rays are cast to the player's
    // hitbox, whatever they hit is segmented into connected shapes, and shapes are removed whole,
    // nearest the player first. A shape that could not be fully resolved is skipped rather than
    // partly cut away, so running out of budget removes fewer shapes instead of degrading into a
    // cylinder.

    /** Master toggle for shape culling. When off, nothing between camera and player is removed. */
    @SerialEntry
    public boolean shapeCulling = true;
    /**
     * Cut a silhouette opening through whatever is in the way, rather than detecting and removing
     * whole objects. On by default.
     *
     * <p>Object detection is the more appealing idea and lost on the evidence. Deciding where one
     * tree ends has no reliable local answer once canopies touch, and each bound that fixed one
     * case — trunk-relative leash, size caps, separate width and height extents — exposed the next.
     * The silhouette is bounded by construction, always resolves, and never has to answer the
     * question at all.
     *
     * <p>What made whole-object removal worth pursuing was avoiding a bore through solid geometry.
     * Ghosting removed blocks back translucently answered that directly, so the opening reads as
     * the tree going see-through rather than as damage.
     *
     * <p>Turn off to go back to object detection, governed by the tree caps and extent limits
     * below, with anything that fails to resolve still falling through to the silhouette.
     */
    @SerialEntry
    public boolean unifiedSilhouette = true;
    /**
     * Fraction of sample rays a shape must block before it counts as hiding the player. Raise it
     * to keep more standing, lower it to cull more aggressively.
     */
    @SerialEntry
    public float terrainOccludeThreshold = 0.25F;
    /** Above this fraction of unobstructed rays, nothing is culled at all. */
    @SerialEntry
    public float sightlineSuppressThreshold = 0.9F;
    /**
     * Blocks per shape before the flood gives up and marks it unresolved. Raise it to let larger
     * structures qualify; terrain is one endless component and will always exceed it, which is how
     * hillsides end up left alone.
     */
    @SerialEntry
    public int terrainShapeCap = 2048;
    /**
     * Whether natural ground can seed a silhouette opening. Terrain is one connected mass to the
     * horizon, so it has no "whole shape" to remove; instead the blocks actually standing in the
     * way are removed, grown outward by the dilation below. Turn this off to leave hillsides and
     * cliffs untouched and remove only trees and built structures.
     *
     * <p>Note this gates what may <i>start</i> an opening, not what the opening may grow through —
     * once open it spreads through any block type, so it does not leave leaves or planks hanging in
     * the middle of a hole. Objects the component path resolved are claimed first and stay whole.
     */
    @SerialEntry
    public boolean terrainSilhouetteCulling = true;
    /** How far the terrain opening grows beyond the blocks directly in the way, in blocks. */
    @SerialEntry
    public int terrainSilhouetteDilation = 8;
    /**
     * Blocks per tree shape. Canopies are large and interlocking ones merge into a single shape,
     * so this needs far more headroom than built structures do.
     */
    @SerialEntry
    public int treeShapeCap = 6144;
    /**
     * Widest a shape may get horizontally, in blocks, before it is judged unresolved.
     *
     * <p>Bounded by the shape's own extent rather than by a radius from wherever the ray happened
     * to hit it, which used to make the verdict depend on the seed: a large jungle tree is thirty
     * blocks tall, so a seed at the trunk base put the far canopy right on the limit while a seed
     * in the canopy passed comfortably.
     *
     * <p>Width and height are separate because that is what actually distinguishes a tree from a
     * forest. Trees are tall and narrow; a canopy sprawling from tree to tree is wide and flat, so
     * it trips this limit long before it trips the height one.
     */
    @SerialEntry
    public int shapeMaxSpan = 24;
    /** Tallest a shape may get, in blocks. Generous, so that large jungle trees still resolve. */
    @SerialEntry
    public int shapeMaxHeight = 48;
    /**
     * How many blocks of leaves may separate a canopy block from the nearest log before the flood
     * stops. This is what keeps one tree from becoming a forest: neighbouring canopies touch, and
     * diagonal connectivity follows them, so without it a single flood walks tree to tree until it
     * blows its leash and the whole merged blob is discarded as unresolved. Raise for unusually
     * broad canopies; lower if adjacent trees are still being taken together.
     */
    @SerialEntry
    public int treeLeafSpread = 5;
    /**
     * Draw removed blocks back as translucent geometry instead of leaving a void.
     *
     * <p>Without this, removing everything between the camera and a buried player carves a shaft to
     * the surface and you are looking into a black pit. Ghosting draws the removed volume back with
     * opacity rising with distance from the player, so the world still reads as intact and only the
     * pocket around the player is genuinely see-through.
     */
    @SerialEntry
    public boolean ghostCulledBlocks = true;
    /**
     * Radius around the player that stays completely clear, measured <b>on screen</b> as a fraction
     * of screen height. Screen distance rather than world distance: under an isometric camera a
     * world-space sphere projects to an ellipse, so blocks far along the view axis sit near the
     * player in 3D while being nowhere near them as drawn.
     */
    @SerialEntry
    public float ghostClearScreen = 0.20F;
    /** Screen distance, in half screen heights, at which the ghost reaches full opacity. */
    @SerialEntry
    public float ghostOpaqueScreen = 1.40F;
    /** Opacity the ghost tops out at. Below 1 it always reads as ghosted rather than solid. */
    @SerialEntry
    public float ghostMaxAlpha = 0.90F;
    /** Most shapes removed at once. */
    @SerialEntry
    public int sightlineMaxShapes = 24;
    /** Most blocks removed at once, across all shapes. */
    @SerialEntry
    public int sightlineMaxCulledBlocks = 8192;
    /** Minimum ticks between sightline casts. */
    @SerialEntry
    public int sightlineRescanCooldown = 2;
    /** Camera movement, in blocks, that forces a fresh cast before the cooldown elapses. */
    @SerialEntry
    public float sightlineCameraStep = 0.75F;

    @SerialEntry
    public boolean scrollWheelZoom = true;
    @SerialEntry
    public boolean dynamicCamera = false;
    @SerialEntry
    public boolean forceNoDefer =  false;
    @SerialEntry
    public boolean cameraRelative =  true;

    /**
     * Native gamepad left-stick movement. When on (and a controller is connected) the left
     * analog stick drives movement through the same camera-relative pipeline as WASD; while the
     * stick is deflected past the deadzone it overrides the keyboard for that tick.
     */
    @SerialEntry
    public boolean joystickMovement =  true;

    @SerialEntry
    public boolean turnToMouse =  true;
    @SerialEntry
    public boolean clipToSpace =  false;

    @SerialEntry
    public boolean additionalMeleeAssistance =  false;
    @SerialEntry
    public boolean contextualTargeting =  false;
    @SerialEntry
    public boolean movementTargeting =  false;
    @SerialEntry
    public boolean contextualInteract =  false;
    @SerialEntry
    public boolean forceAutoJump =  true;
    @SerialEntry
    public boolean rollTowardsCursor =  true;

    @SerialEntry

    public float moveFactor_v3 = 0.5F;
    @SerialEntry
    public float fov = 45.0F;
    @SerialEntry
    public float zoomFactor = 1.5F;
    @SerialEntry
    public float zNearFactor = 1F;

    @SerialEntry
    public boolean ortho = false;
    @SerialEntry

    public boolean clickToMove = false;

    @SerialEntry

    public boolean frustumCulling = true;

    @SerialEntry
    public boolean showFirstTimeGui = true;

    @SerialEntry
    public float soundListenerBias = 0.66F;

    // --- Effective accessors -----------------------------------------------------------------
    // Read these (not the raw fields) at behavior sites so "Controller mode" can override them.

    /** Turn-to-mouse facing; forced OFF in Controller mode (movement drives facing instead). */
    public boolean isTurnToMouse() {
        return !controllerMode && turnToMouse;
    }

    /** Click-to-move; forced OFF in Controller mode. */
    public boolean isClickToMove() {
        return !controllerMode && clickToMove;
    }

    /** Contextual combat targeting; forced ON in Controller mode. */
    public boolean isContextualTargeting() {
        return controllerMode || contextualTargeting;
    }

    /** Movement-based contextual targeting; forced ON in Controller mode. */
    public boolean isMovementTargeting() {
        return controllerMode || movementTargeting;
    }

    /** Contextual interactable-block targeting; forced ON in Controller mode. */
    public boolean isContextualInteract() {
        return controllerMode || contextualInteract;
    }

    /** Combat Roll compat "roll towards cursor"; forced OFF in Controller mode. */
    public boolean isRollTowardsCursor() {
        return !controllerMode && rollTowardsCursor;
    }

    /** Native gamepad left-stick movement; forced ON in Controller mode. */
    public boolean isJoystickMovement() {
        return controllerMode || joystickMovement;
    }
}
