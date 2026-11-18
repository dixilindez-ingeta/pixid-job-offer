package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.util.UUID;

public record JobOfferId(UUID value) {
    public JobOfferId {
        Guard.notNull(value, "JobOfferId cannot be null");
    }

    public static JobOfferId generate() {
        return new JobOfferId(UUID.randomUUID());
    }
}
