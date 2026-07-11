package com.suyash.smarturl.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortenUrlResponse {

    private String shortUrl;

    private String shortCode;

    private String originalUrl;

    private String expiresAt;

}