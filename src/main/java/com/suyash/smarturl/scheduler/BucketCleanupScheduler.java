package com.suyash.smarturl.scheduler;

import com.suyash.smarturl.ratelimiter.RateLimiterService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BucketCleanupScheduler {

    private final RateLimiterService rateLimiterService;

    public BucketCleanupScheduler(
            RateLimiterService rateLimiterService) {

        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Cleans expired buckets every 10 minutes.
     */
    @Scheduled(fixedDelay = 600000)
    public void cleanupBuckets() {
        rateLimiterService.cleanup();
    }
}