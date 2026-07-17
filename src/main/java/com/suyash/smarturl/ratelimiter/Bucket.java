package com.suyash.smarturl.ratelimiter;

/**
 * Represents a generic rate limiting bucket.
 * Implementations must be thread-safe.
 */
public interface Bucket {

    /**
     * Attempts to consume one token.
     * 
     * @return true if request is allowed.
     */
    boolean tryConsume();

    /**
     * Remaining tokens available.
     * 
     * @return remaining tokens
     */
    long getRemainingTokens();

    /**
     * Seconds until next token becomes available.
     * 
     * @return retry-after seconds
     */
    long getRetryAfterSeconds();

    /**
     * Returns true if this bucket has been inactive long enough
     * to be removed from memory.
     * 
     * @return whether bucket expired
     */
    boolean isExpired();

}