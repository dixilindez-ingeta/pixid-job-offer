package com.pixid.joboffer.presentation.joboffer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.domain.valueobject.TransitionAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobOfferResponse(
        UUID id,
        UUID companyId,
        String title,
        JobOfferStatus status,
        LocationDto location,
        CompensationDto compensation,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        @Schema(description = "Actions exécutables dans l'état courant (absent des listes)", example = "[\"SUBMIT\"]")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<TransitionAction> availableTransitions
) {
}
