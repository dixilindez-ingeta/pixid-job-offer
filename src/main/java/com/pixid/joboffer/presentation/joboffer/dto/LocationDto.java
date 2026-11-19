package com.pixid.joboffer.presentation.joboffer.dto;

import com.pixid.joboffer.domain.valueobject.LocationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LocationDto(
        @Schema(example = "CUSTOM") @NotNull LocationType type,
        @Valid AddressDto address
) {
}
