package com.pixid.joboffer.presentation.company;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CompanyConfigRequest(
        @Schema(example = "true") @NotNull Boolean approvalRequired,
        @Schema(example = "false") @NotNull Boolean partialSaveEnabled,
        @Schema(example = "false") @NotNull Boolean manualPostingRequired
) {
}
