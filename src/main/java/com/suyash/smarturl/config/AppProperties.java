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

    private Qr qr = new Qr();

    @Getter
    @Setter
    public static class Qr {

        private int width = 300;

        private int height = 300;

        private String imageFormat = "PNG";

    }

}