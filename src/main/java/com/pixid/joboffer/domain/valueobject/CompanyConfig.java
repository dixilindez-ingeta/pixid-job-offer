package com.pixid.joboffer.domain.valueobject;

public record CompanyConfig(
        boolean approvalRequired,
        boolean partialSaveEnabled,
        boolean manualPostingRequired
) {
    public static CompanyConfig defaults() {
        return new CompanyConfig(false, false, false);
    }
}
