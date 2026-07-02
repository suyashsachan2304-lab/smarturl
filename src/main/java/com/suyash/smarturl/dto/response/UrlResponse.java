package com.suyash.smarturl.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UrlResponse {

    private Long id;

    private String originalUrl;

    private String shortCode;

    private Long clickCount;

    private Boolean active;

    private LocalDateTime createdAt;

}