package com.cleannrooster.dungeons_iso.api.cullers.room;

import com.cleannrooster.dungeons_iso.config.Config;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Rate-limits chunk section re-meshing.
 *
 * <p>The flood itself is cheap — tens of thousands of cells at a few tens of nanoseconds each,
 * once, on a worker thread. Re-meshing is the expensive part: one to ten milliseconds per section,
 * and a room roof spans twenty to forty of them. Dumping that into a single tick is a visible
 * hitch, so sections are drip-fed a few per tick instead, nearest to the player first, which reads
 * as a deliberate wipe rather than a stutter.
 *
 * <p>This queue is shared by every culling tier on purpose. Separate budgets would let the room
 * roof and any other system spike each other. Client thread only.
 */
public final class SectionRebuildQueue {

    public static final SectionRebuildQueue INSTANCE = new SectionRebuildQueue();

    /** Insertion-ordered so the distance sort applied at submit time survives until drain. */
    private final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();

    private SectionRebuildQueue() {
    }

    /**
     * Queues sections for rebuild, closest to {@code near} first. Sections already queued keep
     * their original position rather than jumping the line.
     */
    public void submit(LongSet sections, BlockPos near) {
        if (sections == null || sections.isEmpty()) {
            return;
        }

        LongArrayList sorted = new LongArrayList(sections);
        int px = near.getX() >> 4;
        int py = near.getY() >> 4;
        int pz = near.getZ() >> 4;

        // Explicit LongComparator: a bare lambda is ambiguous between LongList.sort(LongComparator)
        // and List.sort(Comparator<? super Long>).
        LongComparator byDistance =
                (a, b) -> Integer.compare(distSq(a, px, py, pz), distSq(b, px, py, pz));
        sorted.sort(byDistance);

        for (LongIterator it = sorted.iterator(); it.hasNext(); ) {
            this.queue.add(it.nextLong());
        }
    }

    /** Rebuilds up to the configured number of sections. Call once per client tick. */
    public void drain() {
        if (this.queue.isEmpty()) {
            return;
        }

        int budget = Math.max(1, Math.min(64, Config.GSON.instance().roomSectionsPerTick));
        ChunkRebuildScheduler scheduler = ChunkRebuildScheduler.get();

        for (int i = 0; i < budget && !this.queue.isEmpty(); i++) {
            long packed = this.queue.removeFirstLong();
            scheduler.scheduleSection(
                    ChunkSectionPos.unpackX(packed),
                    ChunkSectionPos.unpackY(packed),
                    ChunkSectionPos.unpackZ(packed));
        }
    }

    public void clear() {
        this.queue.clear();
    }

    public int size() {
        return this.queue.size();
    }

    /**
     * The sections whose content actually differs between two hashed section maps.
     *
     * <p>Queuing the union of the old and new section sets re-meshes everything either result
     * touched, but between two scans a fraction of a second apart most sections are byte-identical
     * — so nearly all of that work rebuilds a mesh into exactly what it already was.
     */
    public static LongOpenHashSet diff(Long2LongMap before, Long2LongMap after) {
        LongOpenHashSet dirty = new LongOpenHashSet();

        for (Long2LongMap.Entry e : after.long2LongEntrySet()) {
            long key = e.getLongKey();
            if (!before.containsKey(key) || before.get(key) != e.getLongValue()) {
                dirty.add(key);
            }
        }
        for (Long2LongMap.Entry e : before.long2LongEntrySet()) {
            long key = e.getLongKey();
            if (!after.containsKey(key)) {
                // Dropped from the result, so whatever it was hiding has to come back.
                dirty.add(key);
            }
        }
        return dirty;
    }

    /** splitmix64 finaliser. Combined by addition so section hashes are order-independent. */
    public static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xFF51AFD7ED558CCDL;
        z = (z ^ (z >>> 33)) * 0xC4CEB9FE1A85EC53L;
        return z ^ (z >>> 33);
    }

    private static int distSq(long packed, int px, int py, int pz) {
        int dx = ChunkSectionPos.unpackX(packed) - px;
        int dy = ChunkSectionPos.unpackY(packed) - py;
        int dz = ChunkSectionPos.unpackZ(packed) - pz;
        return dx * dx + dy * dy + dz * dz;
    }
}
