package com.suyash.smarturl.constants;

public final class AppConstants {

    private AppConstants() {
        throw new IllegalStateException("Utility class");
    }

    // API
    public static final String API_BASE = "/api/v1";
    public static final String URL_API = API_BASE + "/urls";

    // Cache
    public static final String URL_CACHE = "url-cache";

    // Headers
    public static final String LOCATION = "Location";

    // Messages
    public static final String URL_CREATED = "Short URL created successfully.";
    public static final String URL_NOT_FOUND = "Short URL does not exist.";
    public static final String INVALID_URL = "Invalid URL.";
    public static final String URL_EXPIRED = "Short URL has expired.";

    // Regex
    public static final String URL_REGEX =
            "^(https?:\\/\\/)([\\w.-]+)(:[0-9]+)?(\\/.*)?$";

    // Short URL
    public static final String BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static final int MAX_SHORT_CODE_LENGTH = 10;
    public static final int MIN_SHORT_CODE_LENGTH = 6;
}