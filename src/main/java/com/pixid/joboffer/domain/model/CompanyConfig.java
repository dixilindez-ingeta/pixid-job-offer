package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.validator.Guard;
import com.pixid.joboffer.domain.valueobject.CompanyId;

public record CompanyConfig(
        CompanyId companyId,
        boolean approvalRequired,
        boolean partialSaveEnabled,
        boolean manualPostingRequired
) {
    public CompanyConfig {
        Guard.notNull(companyId, "CompanyId is required for CompanyConfig");
    }

    public static CompanyConfig defaultFor(CompanyId companyId) {
        return new CompanyConfig(companyId, false, false, false);
    }
}
