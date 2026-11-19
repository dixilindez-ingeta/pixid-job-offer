package com.pixid.joboffer.presentation.joboffer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Création d'un brouillon. Les champs de contenu ne sont facultatifs que si l'entreprise a activé partialSaveEnabled.")
public record CreateJobOfferRequest(
        @Schema(example = "3f6c2a9e-1b7d-4c2e-9a1f-5d8e7b6a4c3d") @NotNull UUID companyId,
        @Schema(example = "Préparateur de commandes H/F") @Size(max = 200) String title,
        @Valid LocationDto location,
        @Valid CompensationDto compensation
) {
}
