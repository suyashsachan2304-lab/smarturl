package com.suyash.smarturl.controller;

import com.suyash.smarturl.common.ApiResponse;
import com.suyash.smarturl.constants.AppConstants;
import com.suyash.smarturl.dto.request.ShortenUrlRequest;
import com.suyash.smarturl.dto.response.ShortenUrlResponse;
import com.suyash.smarturl.dto.response.UrlResponse;
import com.suyash.smarturl.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_API)
@Tag(name = "URL APIs", description = "Smart URL REST APIs")
public class UrlController {

        private final UrlService urlService;

        public UrlController(UrlService urlService) {
                this.urlService = urlService;
        }

        @PostMapping
        @Operation(summary = "Create Short URL", description = """
                        Creates a shortened URL.

                        Supports:

                        • Random short code generation

                        • Custom aliases

                        • URL expiration

                        If customAlias is omitted,
                        a random short code is generated.

                        If expiresAt is omitted,
                        the default expiry configuration is used.
                        """)
        public ResponseEntity<ApiResponse<ShortenUrlResponse>> shortenUrl(
                        @Valid @RequestBody ShortenUrlRequest request) {

                ShortenUrlResponse response = urlService.shortenUrl(request);

                ApiResponse<ShortenUrlResponse> apiResponse = ApiResponse.<ShortenUrlResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .success(true)
                                .message(AppConstants.URL_CREATED)
                                .data(response)
                                .build();

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(apiResponse);
        }

        @GetMapping("/{shortCode}/details")
        @Operation(summary = "Get URL Details")
        public ResponseEntity<ApiResponse<UrlResponse>> getUrl(
                        @PathVariable String shortCode) {

                UrlResponse response = urlService.getUrl(shortCode);

                ApiResponse<UrlResponse> apiResponse = ApiResponse.<UrlResponse>builder()
                                .status(HttpStatus.OK.value())
                                .success(true)
                                .message("URL fetched successfully.")
                                .data(response)
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

        @GetMapping("/{shortCode}")
        @Operation(summary = "Redirect to Original URL", description = """
                        Redirects the client to the original URL.

                        Possible responses

                        302 - Redirect

                        404 - Short URL not found

                        410 - Short URL has expired
                        """)
        public ResponseEntity<Void> redirect(
                        @PathVariable String shortCode) {

                String originalUrl = urlService.getOriginalUrl(shortCode);

                return ResponseEntity.status(HttpStatus.FOUND)
                                .location(URI.create(originalUrl))
                                .build();
        }

        @GetMapping
        @Operation(summary = "Get All URLs")
        public ResponseEntity<ApiResponse<List<UrlResponse>>> getAllUrls() {

                List<UrlResponse> response = urlService.getAllUrls();

                ApiResponse<List<UrlResponse>> apiResponse = ApiResponse.<List<UrlResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .success(true)
                                .message("URLs fetched successfully.")
                                .data(response)
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

        @DeleteMapping("/{shortCode}")
        @Operation(summary = "Delete Short URL")
        public ResponseEntity<ApiResponse<Void>> deleteUrl(
                        @PathVariable String shortCode) {

                urlService.deleteUrl(shortCode);

                ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                                .status(HttpStatus.OK.value())
                                .success(true)
                                .message("URL deleted successfully.")
                                .build();

                return ResponseEntity.ok(apiResponse);
        }

}