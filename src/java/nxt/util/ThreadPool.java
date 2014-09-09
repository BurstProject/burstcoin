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
    private static Map<Runnable,Long> backgroundJobs = new HashMap<>();
    private static List<Runnable> beforeStartJobs = new ArrayList<>();
    private static List<Runnable> lastBeforeStartJobs = new ArrayList<>();

    public static synchronized void runBeforeStart(Runnable runnable, boolean runLast) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }
        if (runLast) {
            lastBeforeStartJobs.add(runnable);
        } else {
            beforeStartJobs.add(runnable);
        }
    }

    public static synchronized void scheduleThread(Runnable runnable, int delay) {
        scheduleThread(runnable, delay, TimeUnit.SECONDS);
    }

    public static synchronized void scheduleThread(Runnable runnable, int delay, TimeUnit timeUnit) {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started, no new jobs accepted");
        }
        backgroundJobs.put(runnable, timeUnit.toMillis(delay));
    }

    public static synchronized void start() {
        if (scheduledThreadPool != null) {
            throw new IllegalStateException("Executor service already started");
        }

        Logger.logDebugMessage("Running " + beforeStartJobs.size() + " tasks...");
        runAll(beforeStartJobs);
        beforeStartJobs = null;

        Logger.logDebugMessage("Running " + lastBeforeStartJobs.size() + " final tasks...");
        runAll(lastBeforeStartJobs);
        lastBeforeStartJobs = null;

        Logger.logDebugMessage("Starting " + backgroundJobs.size() + " background jobs");
        scheduledThreadPool = Executors.newScheduledThreadPool(backgroundJobs.size());
        for (Map.Entry<Runnable,Long> entry : backgroundJobs.entrySet()) {
            scheduledThreadPool.scheduleWithFixedDelay(entry.getKey(), 0, entry.getValue(), TimeUnit.MILLISECONDS);
        }
        backgroundJobs = null;
    }

    public static synchronized void shutdown() {
        if (scheduledThreadPool != null) {
            Logger.logDebugMessage("Stopping background jobs...");
            shutdownExecutor(scheduledThreadPool);
            scheduledThreadPool = null;
            Logger.logDebugMessage("...Done");
        }
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

    private static void runAll(List<Runnable> jobs) {
        List<Thread> threads = new ArrayList<>();
        final StringBuffer errors = new StringBuffer();
        for (final Runnable runnable : jobs) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        errors.append(t.getMessage()).append('\n');
                        throw t;
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (errors.length() > 0) {
            throw new RuntimeException("Errors running startup tasks:\n" + errors.toString());
        }
    }

    private ThreadPool() {} //never

}
