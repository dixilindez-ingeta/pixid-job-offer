package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.util.UUID;

public record CompanyId(UUID value) {
    public CompanyId {
        Guard.notNull(value, "CompanyId cannot be null");
    }

    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID());
    }
}
