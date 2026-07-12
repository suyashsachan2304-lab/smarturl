package com.suyash.smarturl.service;

import com.suyash.smarturl.dto.request.ShortenUrlRequest;
import com.suyash.smarturl.dto.response.ShortenUrlResponse;
import com.suyash.smarturl.dto.response.UrlResponse;
import com.suyash.smarturl.entity.UrlMapping;

import java.util.List;

public interface UrlService {

    ShortenUrlResponse shortenUrl(ShortenUrlRequest request);

    UrlResponse getUrl(String shortCode);

    String getOriginalUrl(String shortCode);

    List<UrlResponse> getAllUrls();

    void deleteUrl(String shortCode);

    UrlMapping getActiveUrlMapping(String shortCode);
}