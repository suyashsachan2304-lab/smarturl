package com.suyash.smarturl.ratelimiter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe Token Bucket implementation.
 *
 * Features:
 * - Lazy refill
 * - Burst traffic support
 * - Nanosecond precision
 * - Thread-safe
 * - Automatic expiration support
 */
public class TokenBucket implements Bucket {

    private final long capacity;

    private final long refillTokens;

    private final Duration refillDuration;

    private final Duration bucketExpiry;

    /**
     * Current available tokens.
     */
    private final AtomicLong availableTokens;

    /**
     * Last successful refill time.
     */
    private volatile long lastRefillTimestamp;

    /**
     * Last access time.
     */
    private volatile long lastAccessTimestamp;

    /**
     * Synchronizes refill + consume operations.
     */
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucket(
            long capacity,
            long refillTokens,
            Duration refillDuration,
            Duration bucketExpiry) {

        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDuration = refillDuration;
        this.bucketExpiry = bucketExpiry;

        this.availableTokens = new AtomicLong(capacity);

        long now = System.nanoTime();

        this.lastRefillTimestamp = now;
        this.lastAccessTimestamp = now;
    }

    @Override
    public boolean tryConsume() {

        refill();

        lock.lock();

        try {

            lastAccessTimestamp = System.nanoTime();

            long current = availableTokens.get();

            if (current <= 0) {
                return false;
            }

            availableTokens.decrementAndGet();

            return true;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Refills bucket lazily.
     *
     * Only executes when enough time has elapsed.
     */
    private void refill() {

        long now = System.nanoTime();

        long elapsed = now - lastRefillTimestamp;

        long cycles = BucketRefillStrategy.calculateRefillCycles(
                elapsed,
                refillDuration);

        if (cycles <= 0) {
            return;
        }

        lock.lock();

        try {

            now = System.nanoTime();

            elapsed = now - lastRefillTimestamp;

            cycles = BucketRefillStrategy.calculateRefillCycles(
                    elapsed,
                    refillDuration);

            if (cycles <= 0) {
                return;
            }

            long newTokens = BucketRefillStrategy.refillTokens(
                    availableTokens.get(),
                    refillTokens,
                    cycles,
                    capacity);

            availableTokens.set(newTokens);

            lastRefillTimestamp = lastRefillTimestamp +
                    (cycles * refillDuration.toNanos());

        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getRemainingTokens() {

        refill();

        return availableTokens.get();
    }

    @Override
    public long getRetryAfterSeconds() {

        refill();

        if (availableTokens.get() > 0) {
            return 0;
        }

        long elapsed = System.nanoTime() - lastRefillTimestamp;

        long remaining = refillDuration.toNanos() - elapsed;

        if (remaining <= 0) {
            return 0;
        }

        return Math.max(
                1,
                Duration.ofNanos(remaining).toSeconds());
    }

    @Override
    public boolean isExpired() {

        long inactiveTime = System.nanoTime() - lastAccessTimestamp;

        return inactiveTime >= bucketExpiry.toNanos();
    }

    /**
     * Returns bucket capacity.
     */
    public long getCapacity() {
        return capacity;
    }

    /**
     * Returns configured refill amount.
     */
    public long getRefillTokens() {
        return refillTokens;
    }

    /**
     * Returns refill duration.
     */
    public Duration getRefillDuration() {
        return refillDuration;
    }

    /**
     * Last request timestamp.
     */
    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

}