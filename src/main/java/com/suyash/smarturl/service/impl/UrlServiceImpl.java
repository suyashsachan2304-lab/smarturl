package com.suyash.smarturl.service.impl;

import com.suyash.smarturl.dto.request.ShortenUrlRequest;
import com.suyash.smarturl.dto.response.ShortenUrlResponse;
import com.suyash.smarturl.dto.response.UrlResponse;
import com.suyash.smarturl.entity.UrlMapping;
import com.suyash.smarturl.exception.UrlNotFoundException;
import com.suyash.smarturl.exception.DuplicateAliasException;
import com.suyash.smarturl.exception.UrlExpiredException;
import com.suyash.smarturl.mapper.UrlMapper;
import com.suyash.smarturl.repository.UrlRepository;
import com.suyash.smarturl.service.UrlService;
import com.suyash.smarturl.util.AliasValidator;
import com.suyash.smarturl.util.ShortCodeGenerator;
import com.suyash.smarturl.util.UrlValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UrlMapper urlMapper;
    private final UrlValidator urlValidator;
    private final ShortCodeGenerator shortCodeGenerator;
    private final AliasValidator aliasValidator;

    public UrlServiceImpl(
            UrlRepository urlRepository,
            UrlMapper urlMapper,
            UrlValidator urlValidator,
            ShortCodeGenerator shortCodeGenerator,
            AliasValidator aliasValidator) {

        this.urlRepository = urlRepository;
        this.urlMapper = urlMapper;
        this.urlValidator = urlValidator;
        this.shortCodeGenerator = shortCodeGenerator;
        this.aliasValidator = aliasValidator;
    }

    @Override
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {

        String normalizedUrl = urlValidator.normalize(request.getUrl());

        urlValidator.validate(normalizedUrl);

        request.setUrl(normalizedUrl);

        if (request.getCustomAlias() != null &&
                !request.getCustomAlias().isBlank()) {

            return createShortUrl(request);
        }

        return urlRepository.findByOriginalUrlAndActiveTrue(normalizedUrl)
                .filter(this::isNotExpired)
                .map(urlMapper::toShortenResponse)
                .orElseGet(() -> createShortUrl(request));
    }

    private ShortenUrlResponse createShortUrl(
            ShortenUrlRequest request) {

        String shortCode;

        if (request.getCustomAlias() != null &&
                !request.getCustomAlias().isBlank()) {

            String alias = request.getCustomAlias()
                    .trim()
                    .toLowerCase();

            aliasValidator.validate(alias);

            if (urlRepository.existsByShortCode(alias)) {

                throw new DuplicateAliasException(
                        "Custom alias already exists.");
            }

            shortCode = alias;

            if (urlRepository.existsByShortCodeIgnoreCase(
                    request.getCustomAlias())) {

                throw new DuplicateAliasException(
                        "Custom alias already exists.");
            }

            shortCode = request.getCustomAlias().trim();

        } else {

            shortCode = generateUniqueShortCode();

        }

        UrlMapping entity = urlMapper.toEntity(request, shortCode);

        UrlMapping savedEntity = urlRepository.save(entity);

        return urlMapper.toShortenResponse(savedEntity);
    }

    private String generateUniqueShortCode() {

        String shortCode;

        do {

            shortCode = shortCodeGenerator.generate();

        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    @Override
    public UrlResponse getUrl(String shortCode) {

        UrlMapping entity = getActiveUrlMapping(shortCode);

        return urlMapper.toResponse(entity);
    }

    @Override
    public String getOriginalUrl(String shortCode) {

        UrlMapping entity = getActiveUrlMapping(shortCode);

        entity.setClickCount(entity.getClickCount() + 1);

        urlRepository.save(entity);

        return entity.getOriginalUrl();
    }

    @Override
    public List<UrlResponse> getAllUrls() {

        return urlRepository.findAll()
                .stream()
                .map(urlMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteUrl(String shortCode) {

        UrlMapping entity = getActiveUrlMapping(shortCode);

        urlRepository.delete(entity);
    }

    private UrlMapping getActiveUrlMapping(String shortCode) {

        shortCode = shortCode.trim().toLowerCase();

        UrlMapping entity = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found."));

        if (!isNotExpired(entity)) {
            throw new UrlExpiredException(
                    "Short URL has expired.");
        }

        return entity;
    }

    private boolean isNotExpired(UrlMapping entity) {

        return entity.getExpiresAt() == null
                || entity.getExpiresAt().isAfter(LocalDateTime.now());
    }

}