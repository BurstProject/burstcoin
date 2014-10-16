package nxt.util;

import nxt.Constants;

import java.util.concurrent.atomic.AtomicInteger;

public interface Time {

    int getTime();

    public static final class EpochTime implements Time {

        public int getTime() {
            return (int)((System.currentTimeMillis() - Constants.EPOCH_BEGINNING + 500) / 1000);
        }

    }

    public static final class ConstantTime implements Time {

        private final int time;

        public ConstantTime(int time) {
            this.time = time;
        }

        public int getTime() {
            return time;
        }

    }

    public static final class FasterTime implements Time {

        private final int multiplier;
        private final long systemStartTime;
        private final int time;

        public FasterTime(int time, int multiplier) {
            if (multiplier > 1000 || multiplier <= 0) {
                throw new IllegalArgumentException("Time multiplier must be between 1 and 1000");
            }
            this.multiplier = multiplier;
            this.time = time;
            this.systemStartTime = System.currentTimeMillis();
        }

        public int getTime() {
            return time + (int)((System.currentTimeMillis() - systemStartTime) / (1000 / multiplier));
        }

    }

    public static final class CounterTime implements Time {

        private final AtomicInteger counter;

        public CounterTime(int time) {
            this.counter = new AtomicInteger(time);
        }

        public int getTime() {
            return counter.incrementAndGet();
        }

    }

}
