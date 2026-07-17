package com.suyash.smarturl.ratelimiter;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory bucket store.
 *
 * One bucket is maintained per client identifier.
 */
@Component
public class BucketStore {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns an existing bucket or creates a new one.
     */
    public TokenBucket getOrCreate(
            String clientId,
            long capacity,
            long refillTokens,
            Duration refillDuration,
            Duration bucketExpiry) {

        return buckets.computeIfAbsent(
                clientId,
                key -> new TokenBucket(
                        capacity,
                        refillTokens,
                        refillDuration,
                        bucketExpiry));
    }

    /**
     * Remove bucket.
     */
    public void remove(String clientId) {
        buckets.remove(clientId);
    }

    /**
     * Remove expired buckets.
     */
    public void removeExpiredBuckets() {

        buckets.entrySet()
                .removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Number of buckets currently stored.
     */
    public int size() {
        return buckets.size();
    }

    /**
     * Returns all buckets.
     * Useful for monitoring and cleanup.
     */
    public Map<String, TokenBucket> getBuckets() {
        return buckets;
    }

}