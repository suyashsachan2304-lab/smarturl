package com.suyash.smarturl.ratelimiter;

import com.suyash.smarturl.config.RateLimitProperties;
import com.suyash.smarturl.constants.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIdentifier {

    private final RateLimitProperties properties;

    public ClientIdentifier(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns a unique client identifier.
     *
     * Currently:
     * - X-Forwarded-For (optional)
     * - Remote IP
     *
     * Future:
     * - JWT Subject
     * - API Key
     */
    public String getClientId(HttpServletRequest request) {

        if (properties.isTrustProxy()) {

            String forwarded = request.getHeader(AppConstants.X_FORWARDED_FOR);

            if (forwarded != null && !forwarded.isBlank()) {

                return forwarded.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

}