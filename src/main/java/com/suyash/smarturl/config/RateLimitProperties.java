package com.suyash.smarturl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimitProperties {

    private boolean enabled = true;

    private int capacity = 20;

    private int refillTokens = 20;

    private Duration refillDuration = Duration.ofMinutes(1);

    private Duration bucketExpiry = Duration.ofMinutes(30);

    private List<String> excludedPaths = new ArrayList<>();

    private boolean trustProxy = false;

}