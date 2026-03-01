package util;

/**
 * Utility class for tracking algorithm performance metrics.
 *
 * Measures execution time (in milliseconds) and memory usage (in MB)
 * for comparing algorithm efficiency in experiments.
 *
 * Usage:
 *   PerformanceTracker tracker = new PerformanceTracker();
 *   tracker.start();
 *   // ... run algorithm ...
 *   tracker.stop();
 *   System.out.println("Time: " + tracker.getElapsedTimeMs() + " ms");
 *   System.out.println("Memory: " + tracker.getMemoryUsageMB() + " MB");
 *
 * @author [Your Name]
 */
public class PerformanceTracker {

    /** Time when tracking started (nanoseconds) */
    private long startTime;

    /** Time when tracking stopped (nanoseconds) */
    private long endTime;

    /** Memory used at start (bytes) */
    private long startMemory;

    /** Peak memory observed during tracking (bytes) */
    private long peakMemory;

    /**
     * Starts the performance tracking.
     * Records the current time and triggers garbage collection
     * for more accurate memory measurement.
     */
    public void start() {
        // Force GC for accurate memory baseline
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }

        Runtime runtime = Runtime.getRuntime();
        startMemory = runtime.totalMemory() - runtime.freeMemory();
        peakMemory = startMemory;
        startTime = System.nanoTime();
    }

    /**
     * Stops the performance tracking and records final metrics.
     */
    public void stop() {
        endTime = System.nanoTime();

        Runtime runtime = Runtime.getRuntime();
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        if (currentMemory > peakMemory) {
            peakMemory = currentMemory;
        }
    }

    /**
     * Updates peak memory tracking. Call this periodically during
     * long-running algorithms for more accurate peak measurement.
     */
    public void checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        if (currentMemory > peakMemory) {
            peakMemory = currentMemory;
        }
    }

    /**
     * Returns the elapsed time in milliseconds.
     *
     * @return elapsed time in ms
     */
    public long getElapsedTimeMs() {
        return (endTime - startTime) / 1_000_000;
    }

    /**
     * Returns the elapsed time in seconds with decimal precision.
     *
     * @return elapsed time in seconds
     */
    public double getElapsedTimeSec() {
        return (endTime - startTime) / 1_000_000_000.0;
    }

    /**
     * Returns the memory usage in megabytes.
     * Computed as peak memory minus starting memory.
     *
     * @return memory usage in MB
     */
    public double getMemoryUsageMB() {
        return (peakMemory - startMemory) / (1024.0 * 1024.0);
    }

    /**
     * Returns a summary string of the performance metrics.
     *
     * @return formatted performance summary
     */
    @Override
    public String toString() {
        return String.format("Time: %d ms (%.2f sec), Memory: %.2f MB",
                getElapsedTimeMs(), getElapsedTimeSec(), getMemoryUsageMB());
    }
}