package com.suyash.smarturl.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

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

    @Size(min = 4, max = 20)
    private String customAlias;

}