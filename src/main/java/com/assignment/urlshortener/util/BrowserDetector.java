package com.assignment.urlshortener.util;

public final class BrowserDetector {

    private BrowserDetector() {
    }

    public static String detect(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/") || userAgent.contains("Edge/")) {
            return "Edge";
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return "Opera";
        }
        if (userAgent.contains("Chrome/")) {
            return "Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Firefox";
        }
        if (userAgent.contains("Safari/")) {
            return "Safari";
        }
        return "Unknown";
    }

    public static String detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            return "Tablet";
        }
        if (userAgent.contains("Mobile") || userAgent.contains("Android")) {
            return "Mobile";
        }
        return "Desktop";
    }

    public static String detectOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Mac OS X") || userAgent.contains("Macintosh")) {
            return "macOS";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
