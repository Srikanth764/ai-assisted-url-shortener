package com.assignment.urlshortener.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

public class StrictHttpUrlValidator implements ConstraintValidator<StrictHttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank() || value.length() > 2048) {
            return true;
        }
        if (containsUnsafeCharacters(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null
                    && uri.getPort() <= 65535;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean containsUnsafeCharacters(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)
                    || character == '<' || character == '>' || character == '"'
                    || character == '\'' || character == '`') {
                return true;
            }
        }
        return false;
    }
}