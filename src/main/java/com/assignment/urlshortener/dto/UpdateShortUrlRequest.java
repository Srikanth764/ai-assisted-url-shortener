package com.assignment.urlshortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import com.assignment.urlshortener.validation.StrictHttpUrl;

import java.time.Instant;

public record UpdateShortUrlRequest(
        @NotBlank
        @Size(max = 2048)
        @StrictHttpUrl
        @URL(regexp = "^(?i)https?://.+", message = "originalUrl must be a well-formed HTTP or HTTPS URL")
        String originalUrl,
        @Future(message = "expiresAt must be in the future")
        Instant expiresAt
) {
    public UpdateShortUrlRequest {
        if (originalUrl != null) {
            originalUrl = originalUrl.trim();
        }
    }
}