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

    @Size(
            min = 4,
            max = 20,
            message = "Custom alias must be between 4 and 20 characters."
    )
    private String customAlias;

    @Future(message = "Expiry date must be in the future.")
    private LocalDateTime expiresAt;

}