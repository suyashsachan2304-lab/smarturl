package com.suyash.smarturl.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortenUrlResponse {

    private String originalUrl;

    private String shortCode;

    private String shortUrl;

}