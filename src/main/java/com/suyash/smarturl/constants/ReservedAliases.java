package com.suyash.smarturl.constants;

import java.util.Set;

public final class ReservedAliases {

    private ReservedAliases() {
    }

    public static final Set<String> RESERVED = Set.of(
            "api",
            "swagger-ui",
            "swagger",
            "actuator",
            "health",
            "login",
            "logout",
            "admin",
            "docs",
            "favicon.ico",
            "error",
            "v1",
            "urls"
    );

}