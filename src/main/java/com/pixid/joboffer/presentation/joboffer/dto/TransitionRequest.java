package com.pixid.joboffer.presentation.joboffer.dto;

import com.pixid.joboffer.domain.valueobject.TransitionAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@Schema(example = "SUBMIT") @NotNull TransitionAction action) {
}
