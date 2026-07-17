package com.suyash.smarturl.ratelimiter;

import com.suyash.smarturl.config.RateLimitProperties;
import com.suyash.smarturl.constants.AppConstants;
import com.suyash.smarturl.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ClientIdentifier clientIdentifier;
    private final RateLimitProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitInterceptor(
            RateLimiterService rateLimiterService,
            ClientIdentifier clientIdentifier,
            RateLimitProperties properties) {

        this.rateLimiterService = rateLimiterService;
        this.clientIdentifier = clientIdentifier;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        if (!properties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();

        for (String excluded : properties.getExcludedPaths()) {
            if (pathMatcher.match(excluded, path)) {
                return true;
            }
        }

        String clientId = clientIdentifier.getClientId(request);

        boolean allowed = rateLimiterService.allowRequest(clientId);

        if (!allowed) {

            long retryAfter = rateLimiterService.getRetryAfter(clientId);

            response.setHeader(
                    AppConstants.RETRY_AFTER,
                    String.valueOf(retryAfter));

            throw new RateLimitExceededException(
                    AppConstants.TOO_MANY_REQUESTS,
                    retryAfter);
        }

        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(
                        rateLimiterService.getRemainingTokens(clientId)));

        return true;
    }
}