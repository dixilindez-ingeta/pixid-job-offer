package com.pixid.joboffer.presentation.company;

import java.util.UUID;

public record CompanyConfigResponse(
        UUID companyId,
        boolean approvalRequired,
        boolean partialSaveEnabled,
        boolean manualPostingRequired
) {
}
