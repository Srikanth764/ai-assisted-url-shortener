package com.assignment.urlshortener.service;

import com.assignment.urlshortener.dto.CreateShortUrlRequest;
import com.assignment.urlshortener.dto.CreateShortUrlResponse;
import com.assignment.urlshortener.dto.ManagedUrlResponse;
import com.assignment.urlshortener.dto.UpdateShortUrlRequest;
import com.assignment.urlshortener.entity.ShortUrl;
import com.assignment.urlshortener.exception.CustomAliasConflictException;
import com.assignment.urlshortener.exception.ShortCodeGenerationException;
import com.assignment.urlshortener.exception.ShortUrlAlreadyInactiveException;
import com.assignment.urlshortener.exception.ShortUrlExpiredException;
import com.assignment.urlshortener.exception.ShortUrlNotFoundException;
import com.assignment.urlshortener.repository.ShortUrlRepository;
import com.assignment.urlshortener.util.ShortCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "actuator", "health", "admin", "login", "logout", "docs", "swagger");

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final String baseUrl;

    public UrlShortenerService(ShortUrlRepository shortUrlRepository,
                                ShortCodeGenerator shortCodeGenerator,
                                @Value("${app.base-url}") String baseUrl) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public CreateShortUrlResponse createShortUrl(CreateShortUrlRequest request) {
        String shortCode = request.customAlias() != null
                ? reserveCustomAlias(request.customAlias())
                : generateUniqueShortCode();
        Instant createdAt = Instant.now();

        ShortUrl shortUrl = new ShortUrl(request.originalUrl(), shortCode, createdAt, request.expiresAt());
        try {
            shortUrlRepository.save(shortUrl);
        } catch (DataIntegrityViolationException ex) {
            if (request.customAlias() != null) {
                log.warn("Custom alias conflict on save: {}", shortCode);
                throw new CustomAliasConflictException(shortCode);
            }
            throw ex;
        }

        log.info("Created short URL with code {}", shortCode);
        return new CreateShortUrlResponse(shortCode, buildShortUrl(shortCode), request.originalUrl(), createdAt,
                request.expiresAt());
    }

    @Transactional
    public String resolveOriginalUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .filter(ShortUrl::isActive)
                .orElseThrow(() -> {
                    log.warn("Short code not found or inactive: {}", shortCode);
                    return new ShortUrlNotFoundException(shortCode);
                });

        if (shortUrl.isExpired()) {
            log.warn("Short code expired: {}", shortCode);
            throw new ShortUrlExpiredException(shortCode);
        }

        shortUrlRepository.incrementClickCount(shortCode, Instant.now());
        return shortUrl.getOriginalUrl();
    }

        @Transactional(readOnly = true)
        public List<ManagedUrlResponse> getManagedUrls() {
        return shortUrlRepository.findAll().stream()
            .map(shortUrl -> new ManagedUrlResponse(
                shortUrl.getShortCode(),
                buildShortUrl(shortUrl.getShortCode()),
                shortUrl.getOriginalUrl(),
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.isActive()))
            .collect(Collectors.toList());
        }

    @Transactional
    public void deactivateShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> {
                    log.warn("Short code not found: {}", shortCode);
                    return new ShortUrlNotFoundException(shortCode);
                });

        if (!shortUrl.isActive()) {
            log.warn("Short code already inactive: {}", shortCode);
            throw new ShortUrlAlreadyInactiveException(shortCode);
        }

        shortUrl.deactivate();
        shortUrlRepository.save(shortUrl);

        log.info("Deactivated short URL with code {}", shortCode);
    }

    @Transactional
    public void deleteShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        shortUrlRepository.delete(shortUrl);
        log.info("Deleted short URL with code {}", shortCode);
    }

    @Transactional
    public void updateShortUrl(String shortCode, UpdateShortUrlRequest request) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        shortUrl.updateDestination(request.originalUrl(), request.expiresAt());
        shortUrlRepository.save(shortUrl);
        log.info("Updated short URL with code {}", shortCode);
    }

    private String reserveCustomAlias(String alias) {
        if (RESERVED_ALIASES.contains(alias)) {
            log.warn("Rejected reserved custom alias: {}", alias);
            throw new CustomAliasConflictException(alias);
        }
        if (shortUrlRepository.existsByShortCode(alias)) {
            log.warn("Custom alias already in use: {}", alias);
            throw new CustomAliasConflictException(alias);
        }
        return alias;
    }

    private String generateUniqueShortCode() {
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = shortCodeGenerator.generate();
            if (!shortUrlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
            log.warn("Short code collision on attempt {}: {}", attempt, candidate);
        }
        throw new ShortCodeGenerationException(
                "Failed to generate a unique short code after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    private String buildShortUrl(String shortCode) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/" + shortCode;
    }
}
