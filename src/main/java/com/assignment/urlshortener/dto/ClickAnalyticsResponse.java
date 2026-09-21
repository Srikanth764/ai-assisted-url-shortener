package com.assignment.urlshortener.dto;

import java.util.Map;
import java.util.Collections;

public record ClickAnalyticsResponse(
        String shortCode,
        long totalEvents,
                long uniqueVisitors,
                Map<String, Long> byBrowser,
                Map<String, Long> byDevice,
                Map<String, Long> byOperatingSystem,
                Map<String, Long> byCountry
) {
        public ClickAnalyticsResponse(String shortCode, long totalEvents, Map<String, Long> byBrowser) {
                this(shortCode, totalEvents, totalEvents, byBrowser,
                                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }
}
