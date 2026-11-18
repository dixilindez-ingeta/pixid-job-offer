package com.pixid.joboffer.domain.valueobject;

public record CompanyConfig(
        boolean requireFinalization,
        boolean requireApproval,
        boolean requirePostReview
) {
    public static CompanyConfig defaults() {
        return new CompanyConfig(false, false, false);
    }
}
