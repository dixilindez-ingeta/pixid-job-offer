package com.pixid.joboffer.presentation.joboffer.dto;

import com.pixid.joboffer.domain.valueobject.BonusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record BonusEntryDto(
        @Schema(example = "SIGNING") @NotNull BonusType type,
        @Schema(example = "500.00") @NotNull @PositiveOrZero BigDecimal amount,
        @Schema(description = "ISO 4217", example = "EUR") @NotNull @Pattern(regexp = "[A-Za-z]{3}") String currency
) {
}
