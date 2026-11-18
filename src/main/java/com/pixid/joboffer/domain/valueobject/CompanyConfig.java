package com.pixid.joboffer.domain.valueobject;

public record CompanyConfig(
        boolean requireFinalization,
        boolean requireApproval,
        boolean requirePostReview
) { }