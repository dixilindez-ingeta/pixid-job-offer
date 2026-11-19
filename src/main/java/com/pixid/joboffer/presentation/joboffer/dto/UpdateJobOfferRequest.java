package com.pixid.joboffer.presentation.joboffer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Schema(description = "Mise à jour partielle : un champ absent ou null est laissé inchangé; location et compensation sont remplacées en bloc.")
public record UpdateJobOfferRequest(
        @Schema(example = "Cariste CACES 3 H/F") @Size(max = 200) String title,
        @Valid LocationDto location,
        @Valid CompensationDto compensation
) {
}
