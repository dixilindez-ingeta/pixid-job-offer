package com.pixid.joboffer.presentation.joboffer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Adresse personnalisée (obligatoire si location.type = CUSTOM)")
public record AddressDto(
        @Schema(example = "[\"10 avenue de France\", \"Bâtiment B\"]")
        @Size(max = 3) List<@NotBlank @Size(max = 100) String> lines,
        @Schema(example = "75013") @Size(max = 20) String postalCode,
        @Schema(example = "Paris") @NotBlank @Size(max = 100) String city,
        @Schema(example = "Île-de-France") @Size(max = 100) String region,
        @Schema(description = "ISO 3166-1 alpha-2", example = "FR")
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode
) {
}
