package com.pixid.joboffer.presentation.joboffer;

import com.pixid.joboffer.domain.port.PageRequest.SortDirection;
import com.pixid.joboffer.domain.port.PageRequest.SortField;
import com.pixid.joboffer.presentation.error.BadRequestException;

import java.util.Locale;
import java.util.Map;

/**
 * Parses {@code sort=<field>[,asc|desc]}, e.g. {@code createdAt,desc}.
 */
record SortParameter(SortField field, SortDirection direction) {

    private static final Map<String, SortField> FIELDS = Map.of(
            "createdAt", SortField.CREATED_AT,
            "updatedAt", SortField.UPDATED_AT,
            "publishedAt", SortField.PUBLISHED_AT);

    static SortParameter parse(String value) {
        String[] parts = value.split(",", -1);
        SortField field = FIELDS.get(parts[0].trim());
        if (field == null || parts.length > 2) {
            throw invalid(value);
        }
        if (parts.length == 1) {
            return new SortParameter(field, SortDirection.DESC);
        }
        return switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> new SortParameter(field, SortDirection.ASC);
            case "desc" -> new SortParameter(field, SortDirection.DESC);
            default -> throw invalid(value);
        };
    }

    private static BadRequestException invalid(String value) {
        return new BadRequestException("sort", "Invalid sort '" + value
                + "': expected <createdAt|updatedAt|publishedAt>[,asc|desc]");
    }
}
