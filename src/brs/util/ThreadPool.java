package brs.util;

import brs.props.Props;
import brs.props.PropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool {

  private static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);

  private static ScheduledExecutorService scheduledThreadPool;
  private final  Map<Runnable,Long> backgroundJobs = new HashMap<>();
  private final Map<Runnable,Long> backgroundJobsCores = new HashMap<>();
  private final List<Runnable> beforeStartJobs = new ArrayList<>();
  private final List<Runnable> lastBeforeStartJobs = new ArrayList<>();
  private final List<Runnable> afterStartJobs = new ArrayList<>();

  private final PropertyService propertyService;

  public ThreadPool(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  public synchronized void runBeforeStart(Runnable runnable, boolean runLast) {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started");
    }
    if (runLast) {
      lastBeforeStartJobs.add(runnable);
    } else {
      beforeStartJobs.add(runnable);
    }
  }

  public synchronized void runAfterStart(Runnable runnable) {
    afterStartJobs.add(runnable);
  }

  public synchronized void scheduleThread(String name, Runnable runnable, int delay) {
    scheduleThread(name, runnable, delay, TimeUnit.SECONDS);
  }

  public synchronized void scheduleThread(String name, Runnable runnable, int delay, TimeUnit timeUnit) {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started, no new jobs accepted");
    }
    if (! propertyService.getBoolean("brs.disable" + name + "Thread", false)) {
      backgroundJobs.put(runnable, timeUnit.toMillis(delay));
    } else {
      logger.info("Will not run " + name + " thread");
    }
  }

  public synchronized void scheduleThreadCores(String name, Runnable runnable, int delay) {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started, no new jobs accepted");
    }
    backgroundJobsCores.put(runnable, 1000L * delay);
  }

  public synchronized void start(int timeMultiplier) {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started");
    }

    logger.debug("Running " + beforeStartJobs.size() + " tasks...");
    runAll(beforeStartJobs);
    beforeStartJobs.clear();

    logger.debug("Running " + lastBeforeStartJobs.size() + " final tasks...");
    runAll(lastBeforeStartJobs);
    lastBeforeStartJobs.clear();

    int cores = propertyService.getInt(Props.CPU_NUM_CORES);
    if (cores <= 0) {
        logger.warn("Cannot use 0 cores - defaulting to all available");
        cores = Runtime.getRuntime().availableProcessors();
      }
    int totalThreads = backgroundJobs.size() + backgroundJobsCores.size() * cores;
    logger.debug("Starting " + String.valueOf(totalThreads) + " background jobs");
    scheduledThreadPool = Executors.newScheduledThreadPool(totalThreads);
    for (Map.Entry<Runnable,Long> entry : backgroundJobs.entrySet()) {
      final Runnable inner = entry.getKey();
      Runnable toRun = () -> {
        try {
          inner.run();
        }
        catch (Exception e) {
          logger.warn("Uncaught exception while running background thread "+inner.getClass().getSimpleName(), e);
        }
      };
      scheduledThreadPool.scheduleWithFixedDelay(toRun, 0, Math.max(entry.getValue() / timeMultiplier, 1), TimeUnit.MILLISECONDS);
    }
    backgroundJobs.clear();
	
    // Starting multicore-Threads:
    for (Map.Entry<Runnable,Long> entry : backgroundJobsCores.entrySet()) {
      for (int i=0; i < cores; i++)
        scheduledThreadPool.scheduleWithFixedDelay(entry.getKey(), 0, Math.max(entry.getValue() / timeMultiplier, 1), TimeUnit.MILLISECONDS);
    }
    backgroundJobsCores.clear();

    logger.debug("Starting " + afterStartJobs.size() + " delayed tasks");
    Thread thread = new Thread() {
        @Override
        public void run() {
          runAll(afterStartJobs);
          afterStartJobs.clear();
        }
      };
    thread.setDaemon(true);
    thread.start();
  }

  public synchronized void shutdown() {
    if (scheduledThreadPool != null) {
      logger.info("Stopping background jobs...");
      shutdownExecutor(scheduledThreadPool);
      scheduledThreadPool = null;
      logger.info("...Done");
    }
  }

  public void shutdownExecutor(ExecutorService executor) {
    try {
      if (!executor.isTerminated()) {
        executor.shutdownNow(); //Using shutdown now to stop more execution and to signal within threads to exit.
        if(!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          logger.info("Termination of threads failed.");
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void runAll(List<Runnable> jobs) {
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

}
