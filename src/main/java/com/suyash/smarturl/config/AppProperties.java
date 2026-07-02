package com.suyash.smarturl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;

    private int shortCodeLength = 7;

    private int defaultExpiryDays = 365;

}