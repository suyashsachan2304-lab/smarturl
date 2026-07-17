package com.suyash.smarturl.ratelimiter;

/**
 * Service responsible for deciding whether
 * a request should be allowed.
 */
public interface RateLimiterService {

    /**
     * Attempts to consume one token.
     *
     * @param clientId client identifier
     * @return true if request is allowed
     */
    boolean allowRequest(String clientId);

    /**
     * Seconds until the client may retry.
     *
     * @param clientId client identifier
     * @return retry-after seconds
     */
    long getRetryAfter(String clientId);

    /**
     * Remaining tokens.
     *
     * @param clientId client identifier
     * @return remaining tokens
     */
    long getRemainingTokens(String clientId);

    /**
     * Cleanup expired buckets.
     */
    void cleanup();

}