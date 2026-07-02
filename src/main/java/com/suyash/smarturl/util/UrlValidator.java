package com.suyash.smarturl.util;

import com.suyash.smarturl.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class UrlValidator {

    public void validate(String url) {

        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL cannot be empty.");
        }

        try {

            URI uri = new URI(url);

            if (uri.getScheme() == null) {
                throw new InvalidUrlException("URL must contain a scheme.");
            }

            if (!uri.getScheme().equalsIgnoreCase("http")
                    && !uri.getScheme().equalsIgnoreCase("https")) {

                throw new InvalidUrlException(
                        "Only HTTP and HTTPS URLs are supported."
                );
            }

            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("Invalid host.");
            }

        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL.");
        }

    }

    public String normalize(String url) {

        if (url == null) {
            return null;
        }

        return url.trim();
    }

}