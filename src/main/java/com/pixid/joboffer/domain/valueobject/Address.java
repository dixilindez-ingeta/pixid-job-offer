package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.util.List;

public record Address(
        List<String> lines,
        String postalCode,
        String city,
        String region,
        String countryCode
) {
    public Address {
        Guard.notBlank(city, "City is required for a custom address");
        Guard.validIsoCountry(countryCode, "Country code must be a valid ISO 3166-1 alpha-2 code");

        if (lines != null) {
            Guard.isTrue(lines.size() <= 3, "Address cannot exceed 3 lines");
            for (String line : lines) {
                if (line != null) {
                    Guard.maxLength(line, 100, "Each address line cannot exceed 100 characters");
                }
            }
            lines = List.copyOf(lines);
        } else {
            lines = List.of();
        }
        countryCode = countryCode.toUpperCase();
    }
}