package com.pixid.joboffer.presentation.company;

import com.pixid.joboffer.application.GetCompanyConfig;
import com.pixid.joboffer.application.UpdateCompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/config")
@Tag(name = "Company configuration", description = "Paramétrage du cycle de vie par entreprise")
public class CompanyConfigController {

    private final GetCompanyConfig getCompanyConfig;
    private final UpdateCompanyConfig updateCompanyConfig;

    public CompanyConfigController(GetCompanyConfig getCompanyConfig, UpdateCompanyConfig updateCompanyConfig) {
        this.getCompanyConfig = getCompanyConfig;
        this.updateCompanyConfig = updateCompanyConfig;
    }

    private static CompanyConfigResponse toResponse(UUID companyId, CompanyConfig config) {
        return new CompanyConfigResponse(companyId,
                config.approvalRequired(), config.partialSaveEnabled(), config.manualPostingRequired());
    }

    @GetMapping
    @Operation(summary = "Lire la configuration (valeurs par défaut, toutes à false, si jamais définie)")
    public CompanyConfigResponse get(@PathVariable UUID companyId) {
        return toResponse(companyId, getCompanyConfig.execute(new CompanyId(companyId)));
    }

    @PutMapping
    @Operation(summary = "Remplacer la configuration", responses = {
            @ApiResponse(responseCode = "200", description = "Configuration enregistrée"),
            @ApiResponse(responseCode = "400", description = "Requête invalide",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CompanyConfigResponse update(@PathVariable UUID companyId, @Valid @RequestBody CompanyConfigRequest request) {
        CompanyConfig config = new CompanyConfig(
                request.approvalRequired(), request.partialSaveEnabled(), request.manualPostingRequired());
        return toResponse(companyId, updateCompanyConfig.execute(new CompanyId(companyId), config));
    }
}
