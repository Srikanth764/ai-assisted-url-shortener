package com.assignment.urlshortener.controller;

import com.assignment.urlshortener.dto.ClickAnalyticsResponse;
import com.assignment.urlshortener.dto.CreateShortUrlRequest;
import com.assignment.urlshortener.dto.CreateShortUrlResponse;
import com.assignment.urlshortener.dto.ManagedUrlResponse;
import com.assignment.urlshortener.dto.UpdateShortUrlRequest;
import com.assignment.urlshortener.service.ClickAnalyticsService;
import com.assignment.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final ClickAnalyticsService clickAnalyticsService;

    public UrlController(UrlShortenerService urlShortenerService, ClickAnalyticsService clickAnalyticsService) {
        this.urlShortenerService = urlShortenerService;
        this.clickAnalyticsService = clickAnalyticsService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@Valid @RequestBody CreateShortUrlRequest request) {
        CreateShortUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ManagedUrlResponse>> getManagedUrls() {
        return ResponseEntity.ok(urlShortenerService.getManagedUrls());
    }

    @GetMapping("/{shortCode}/click-analytics")
    public ResponseEntity<ClickAnalyticsResponse> getClickAnalytics(@PathVariable String shortCode) {
        ClickAnalyticsResponse response = clickAnalyticsService.getClickAnalytics(shortCode);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deactivateShortUrl(@PathVariable String shortCode) {
        urlShortenerService.deactivateShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shortCode}/permanent")
    public ResponseEntity<Void> deleteShortUrl(@PathVariable String shortCode) {
        urlShortenerService.deleteShortUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{shortCode}")
    public ResponseEntity<Void> updateShortUrl(@PathVariable String shortCode,
                                                @Valid @RequestBody UpdateShortUrlRequest request) {
        urlShortenerService.updateShortUrl(shortCode, request);
        return ResponseEntity.noContent().build();
    }
}
