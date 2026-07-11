package com.suyash.smarturl.mapper;

import com.suyash.smarturl.config.AppProperties;
import com.suyash.smarturl.dto.request.ShortenUrlRequest;
import com.suyash.smarturl.dto.response.ShortenUrlResponse;
import com.suyash.smarturl.dto.response.UrlResponse;
import com.suyash.smarturl.entity.UrlMapping;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class UrlMapper {

    private final AppProperties appProperties;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UrlMapper(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public UrlMapping toEntity(
            ShortenUrlRequest request,
            String shortCode
    ) {

        LocalDateTime expiryDate =
                request.getExpiresAt() != null
                        ? request.getExpiresAt()
                        : LocalDateTime.now()
                        .plusDays(appProperties.getDefaultExpiryDays());

        return UrlMapping.builder()
                .originalUrl(request.getUrl())
                .shortCode(shortCode)
                .shortUrl(buildShortUrl(shortCode))
                .expiresAt(expiryDate)
                .build();
    }

    public ShortenUrlResponse toShortenResponse(
            UrlMapping entity
    ) {

        return ShortenUrlResponse.builder()
                .shortUrl(entity.getShortUrl())
                .shortCode(entity.getShortCode())
                .originalUrl(entity.getOriginalUrl())
                .expiresAt(format(entity.getExpiresAt()))
                .build();
    }

    public UrlResponse toResponse(
            UrlMapping entity
    ) {

        return UrlResponse.builder()
                .id(entity.getId())
                .originalUrl(entity.getOriginalUrl())
                .shortCode(entity.getShortCode())
                .shortUrl(entity.getShortUrl())
                .clickCount(entity.getClickCount())
                .active(entity.getActive())
                .createdAt(format(entity.getCreatedAt()))
                .expiresAt(format(entity.getExpiresAt()))
                .build();
    }

    private String buildShortUrl(String shortCode) {

        return appProperties.getBaseUrl()
                + "/"
                + shortCode;
    }

    private String format(LocalDateTime dateTime) {

        if (dateTime == null) {
            return null;
        }

        return dateTime.format(FORMATTER);
    }

}