package com.suyash.smarturl.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {

    private Long id;

    private String originalUrl;

    private String shortCode;

    private String shortUrl;

    private Long clickCount;

    private Boolean active;

    private String expiresAt;

    private String createdAt;

}