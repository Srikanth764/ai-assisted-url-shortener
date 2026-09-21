package com.assignment.urlshortener.dto;

import java.time.Instant;

public record ManagedUrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        long clickCount,
        Instant createdAt,
        Instant expiresAt,
        boolean active
) {
}