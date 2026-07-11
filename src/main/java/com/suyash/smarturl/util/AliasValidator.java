package com.suyash.smarturl.util;

import com.suyash.smarturl.constants.ReservedAliases;
import com.suyash.smarturl.exception.InvalidAliasException;
import org.springframework.stereotype.Component;

@Component
public class AliasValidator {

    public void validate(String alias) {

        if (alias == null || alias.isBlank()) {
            return;
        }

        String normalizedAlias = alias.trim().toLowerCase();

        if (ReservedAliases.RESERVED.contains(normalizedAlias)) {
            throw new InvalidAliasException(
                    "Custom alias is reserved."
            );
        }

    }

}