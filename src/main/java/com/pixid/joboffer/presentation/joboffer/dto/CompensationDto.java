package com.pixid.joboffer.presentation.joboffer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Rémunération : au moins une entrée (salaire ou prime)")
public record CompensationDto(
        List<@Valid @NotNull SalaryEntryDto> salaryEntries,
        List<@Valid @NotNull BonusEntryDto> bonusEntries
) {
}
