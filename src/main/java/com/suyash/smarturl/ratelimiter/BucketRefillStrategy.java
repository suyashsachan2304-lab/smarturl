package com.suyash.smarturl.ratelimiter;

import java.time.Duration;

/**
 * Utility responsible for calculating token refills.
 * This class is stateless.
 */
public final class BucketRefillStrategy {

    private BucketRefillStrategy() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Calculates how many refill periods have elapsed.
     * Example:
     * refill duration = 60 sec
     * elapsed = 180 sec
     * returns 3
     * 
     * @param elapsedNanos   elapsed time
     * @param refillDuration refill duration
     * @return elapsed refill periods
     */
    public static long calculateRefillCycles(
            long elapsedNanos,
            Duration refillDuration) {

        if (elapsedNanos <= 0) {
            return 0;
        }

        long refillNanos = refillDuration.toNanos();

        if (refillNanos <= 0) {
            return 0;
        }

        return elapsedNanos / refillNanos;
    }

    /**
     * Calculates the new number of tokens after refill.
     * Tokens never exceed bucket capacity.
     * 
     * @param currentTokens current tokens
     * @param refillTokens  tokens added every cycle
     * @param cycles        elapsed refill cycles
     * @param capacity      bucket capacity
     * @return new token count
     */
    public static long refillTokens(
            long currentTokens,
            long refillTokens,
            long cycles,
            long capacity) {

        if (cycles <= 0) {
            return currentTokens;
        }

        long updated = currentTokens + (cycles * refillTokens);

        return Math.min(updated, capacity);
    }

}