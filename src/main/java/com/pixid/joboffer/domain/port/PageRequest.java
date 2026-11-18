package com.pixid.joboffer.domain.port;

import com.pixid.joboffer.domain.validator.Guard;

public record PageRequest(int page, int size, SortField sortField, SortDirection direction) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        Guard.isTrue(page >= 0, "Page index must be >= 0");
        Guard.isTrue(size >= 1 && size <= MAX_SIZE, "Page size must be between 1 and " + MAX_SIZE);
        Guard.notNull(sortField, "Sort field is required");
        Guard.notNull(direction, "Sort direction is required");
    }

    public enum SortField {CREATED_AT, UPDATED_AT, PUBLISHED_AT}

    public enum SortDirection {ASC, DESC}
}
