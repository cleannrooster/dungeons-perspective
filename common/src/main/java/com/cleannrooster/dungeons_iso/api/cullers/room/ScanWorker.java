package com.cleannrooster.dungeons_iso.api.cullers.room;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * The single background thread both scanners run on.
 *
 * <p>They used to own one each, which was two threads spending almost all of their time parked
 * waiting for a request. One serves both: each scan is short and they are triggered by different
 * things — the room scan by walking to a new block, the sightline cast by moving the camera — so
 * they rarely have work at the same moment, and when they do, one runs immediately after the other.
 *
 * <p>Still exactly one thread touching either scanner's state, so the per-scanner caches and slice
 * bookkeeping stay single-threaded and need no synchronisation, as before.
 *
 * <p>Deliberately a plain daemon thread at minimum priority rather than Minecraft's shared worker
 * pool. These are park-until-work loops, and parking a pooled thread indefinitely would hold a slot
 * in a small pool the game uses for chunk loading; running at minimum priority instead means the
 * scans yield to everything, including Sodium's chunk builders.
 */
public final class ScanWorker {

    /** How long to sleep when neither scanner had anything to do. */
    private static final long IDLE_PARK_NANOS = 2_000_000L;

    /** One unit of background work. Returns true if it actually did something. */
    public interface Job {
        boolean runOnce();
    }

    public static final ScanWorker INSTANCE = new ScanWorker();

    private final List<Job> jobs = new CopyOnWriteArrayList<>();
    private Thread thread;
    private volatile boolean running;

    private ScanWorker() {
    }

    /** Registers a job and makes sure the thread is up. Safe to call repeatedly. */
    public void submit(Job job) {
        if (!this.jobs.contains(job)) {
            this.jobs.add(job);
        }
        ensureRunning();
    }

    private synchronized void ensureRunning() {
        if (this.thread != null && this.thread.isAlive()) {
            return;
        }
        this.running = true;
        this.thread = new Thread(this::loop, "dungeons-iso-scanner");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.start();
    }

    private void loop() {
        while (this.running) {
            boolean worked = false;
            for (Job job : this.jobs) {
                try {
                    worked |= job.runOnce();
                } catch (Throwable ignored) {
                    // A failed scan must never take the thread down with it; the next request will
                    // simply try again.
                }
            }
            if (!worked) {
                LockSupport.parkNanos(IDLE_PARK_NANOS);
            }
        }
    }
}
