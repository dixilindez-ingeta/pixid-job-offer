package com.pixid.joboffer.domain.validator;

import com.pixid.joboffer.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

public final class Guard {

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());

    private Guard() {
        // Classe utilitaire non instanciable
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new DomainValidationException(message);
        }
    }

    public static void notBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(message);
        }
    }

    public static void maxLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new DomainValidationException(message);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new DomainValidationException(message);
        }
    }

    public static void greaterThanOrEqual(BigDecimal value, BigDecimal min, String message) {
        if (value == null || value.compareTo(min) < 0) {
            throw new DomainValidationException(message);
        }
    }

    public static void validIsoCountry(String countryCode, String message) {
        notBlank(countryCode, message);
        if (!COUNTRIES.contains(countryCode.toUpperCase())) {
            throw new DomainValidationException(message);
        }
    }

    public static void validIsoCurrency(String currency, String message) {
        notBlank(currency, message);
        try {
            Currency.getInstance(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException(message);
        }
    }
}