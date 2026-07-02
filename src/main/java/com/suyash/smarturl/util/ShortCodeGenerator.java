package com.suyash.smarturl.util;

import com.suyash.smarturl.config.AppProperties;
import com.suyash.smarturl.constants.AppConstants;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppProperties appProperties;

    public ShortCodeGenerator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generate() {

        int length = appProperties.getShortCodeLength();

        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(
                    AppConstants.BASE62.charAt(
                            RANDOM.nextInt(AppConstants.BASE62.length())
                    )
            );
        }

        return builder.toString();
    }
}