package nxt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool {

    private static ScheduledExecutorService scheduledThreadPool;
    private static Map<Runnable,Integer> backgroundJobs = new HashMap<>();
    private static List<Runnable> runBeforeStartJobs = new ArrayList<>();

    public static synchronized void runBeforeStart(Runnable runnable) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }
        runBeforeStartJobs.add(runnable);
    }

    public static synchronized void scheduleThread(Runnable runnable, int delay) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started, no new jobs accepted");
        }
        backgroundJobs.put(runnable, delay);
    }

    public static synchronized void start() {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }
        Logger.logDebugMessage("Running " + runBeforeStartJobs.size() + " final tasks...");
        for (Runnable runnable : runBeforeStartJobs) {
            runnable.run(); // run them all sequentially within the current thread
        }
        runBeforeStartJobs = null;
        Logger.logDebugMessage("Starting " + backgroundJobs.size() + " background jobs");
        scheduledThreadPool = Executors.newScheduledThreadPool(backgroundJobs.size());
        for (Map.Entry<Runnable,Integer> entry : backgroundJobs.entrySet()) {
            scheduledThreadPool.scheduleWithFixedDelay(entry.getKey(), 0, entry.getValue(), TimeUnit.SECONDS);
        }
        backgroundJobs = null;
    }

    public static synchronized void shutdown() {
        Logger.logDebugMessage("Stopping background jobs...");
        shutdownExecutor(scheduledThreadPool);
        scheduledThreadPool = null;
        Logger.logDebugMessage("...Done");
    }

    public static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! executor.isTerminated()) {
            Logger.logMessage("some threads didn't terminate, forcing shutdown");
            executor.shutdownNow();
        }
    }

    private ThreadPool() {} //never

}
