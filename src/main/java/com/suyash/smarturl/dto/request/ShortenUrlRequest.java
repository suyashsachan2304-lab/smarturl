package com.suyash.smarturl.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenUrlRequest {

    @Pattern(
            regexp = "^(https?://).+",
            message = "Please provide a valid http/https URL"
    )
    private String url;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{4,30}$",
            message = "Custom alias can only contain letters, numbers, hyphens and underscores."
    )
    @Size(
            min = 4,
            max = 30,
            message = "Custom alias must be between 4 and 30 characters."
    )
    private String customAlias;

    @Future(message = "Expiry date must be in the future.")
    private LocalDateTime expiresAt;

}