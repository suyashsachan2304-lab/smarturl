package com.suyash.smarturl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShortenUrlRequest {

    @NotBlank(message = "URL cannot be empty")
    @Pattern(
            regexp = "^(https?://).+",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;

}