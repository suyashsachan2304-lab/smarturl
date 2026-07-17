package com.suyash.smarturl.ratelimiter;

import com.suyash.smarturl.config.RateLimitProperties;
import org.springframework.stereotype.Service;

/**
 * Default implementation of the RateLimiterService.
 *
 * Uses an in-memory Token Bucket implementation.
 */
@Service
public class TokenBucketRateLimiterService implements RateLimiterService {

    private final BucketStore bucketStore;

    private final RateLimitProperties properties;

    public TokenBucketRateLimiterService(
            BucketStore bucketStore,
            RateLimitProperties properties) {

        this.bucketStore = bucketStore;
        this.properties = properties;
    }

    @Override
    public boolean allowRequest(String clientId) {

        if (!properties.isEnabled()) {
            return true;
        }

        TokenBucket bucket = getBucket(clientId);

        return bucket.tryConsume();
    }

    @Override
    public long getRetryAfter(String clientId) {

        if (!properties.isEnabled()) {
            return 0;
        }

        TokenBucket bucket = getBucket(clientId);

        return bucket.getRetryAfterSeconds();
    }

    @Override
    public long getRemainingTokens(String clientId) {

        if (!properties.isEnabled()) {
            return Long.MAX_VALUE;
        }

        TokenBucket bucket = getBucket(clientId);

        return bucket.getRemainingTokens();
    }

    @Override
    public void cleanup() {

        bucketStore.removeExpiredBuckets();
    }

    /**
     * Returns existing bucket or creates one.
     */
    private TokenBucket getBucket(String clientId) {

        return bucketStore.getOrCreate(

                clientId,

                properties.getCapacity(),

                properties.getRefillTokens(),

                properties.getRefillDuration(),

                properties.getBucketExpiry());
    }

    /**
     * Number of active buckets.
     *
     * Useful for monitoring.
     */
    public int getBucketCount() {
        return bucketStore.size();
    }

}