package com.pixid.joboffer.presentation.joboffer;

import com.pixid.joboffer.application.*;
import com.pixid.joboffer.domain.port.JobOfferSearchCriteria;
import com.pixid.joboffer.domain.port.PageRequest;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.presentation.joboffer.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/job-offers")
@Tag(name = "Job offers", description = "Cycle de vie des offres d'emploi")
public class JobOfferController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final CreateJobOffer createJobOffer;
    private final UpdateJobOffer updateJobOffer;
    private final TransitionJobOffer transitionJobOffer;
    private final GetJobOffer getJobOffer;
    private final SearchJobOffers searchJobOffers;

    public JobOfferController(CreateJobOffer createJobOffer, UpdateJobOffer updateJobOffer,
                              TransitionJobOffer transitionJobOffer, GetJobOffer getJobOffer,
                              SearchJobOffers searchJobOffers) {
        this.createJobOffer = createJobOffer;
        this.updateJobOffer = updateJobOffer;
        this.transitionJobOffer = transitionJobOffer;
        this.getJobOffer = getJobOffer;
        this.searchJobOffers = searchJobOffers;
    }

    @PostMapping
    @Operation(summary = "Créer un brouillon d'offre", responses = {
            @ApiResponse(responseCode = "201", description = "Brouillon créé (header Location)"),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou offre incomplète sans partialSaveEnabled",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<JobOfferResponse> create(@Valid @RequestBody CreateJobOfferRequest request) {
        JobOfferDetails details = createJobOffer.execute(new CreateJobOfferCommand(
                new CompanyId(request.companyId()),
                request.title(),
                JobOfferDtoMapper.toLocation(request.location()),
                JobOfferDtoMapper.toCompensation(request.compensation())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(details.jobOffer().getId().value())
                .toUri();
        return ResponseEntity.created(location).body(JobOfferDtoMapper.toResponse(details));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Modifier partiellement une offre (DRAFT ou TO_FINALIZE uniquement)", responses = {
            @ApiResponse(responseCode = "200", description = "Offre modifiée"),
            @ApiResponse(responseCode = "400", description = "Requête invalide",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Offre introuvable",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Offre non modifiable dans son état, ou modification concurrente",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public JobOfferResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateJobOfferRequest request) {
        return JobOfferDtoMapper.toResponse(updateJobOffer.execute(new UpdateJobOfferCommand(
                new JobOfferId(id),
                request.title(),
                JobOfferDtoMapper.toLocation(request.location()),
                JobOfferDtoMapper.toCompensation(request.compensation()))));
    }

    @PostMapping("/{id}/transitions")
    @Operation(summary = "Appliquer une action du cycle de vie",
            description = "Actions : SUBMIT, FINALIZE, APPROVE, REJECT, POST, CLOSE, EXPIRE. "
                    + "Les états désactivés par la configuration de l'entreprise sont sautés.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transition appliquée, offre à jour"),
                    @ApiResponse(responseCode = "400", description = "Action absente ou inconnue",
                            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "404", description = "Offre introuvable",
                            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
                    @ApiResponse(responseCode = "409", description = "Action interdite dans l'état courant, offre incomplète, ou modification concurrente",
                            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
            })
    public JobOfferResponse transition(@PathVariable UUID id, @Valid @RequestBody TransitionRequest request) {
        return JobOfferDtoMapper.toResponse(transitionJobOffer.execute(new JobOfferId(id), request.action()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lire une offre, avec ses transitions disponibles", responses = {
            @ApiResponse(responseCode = "200", description = "Offre trouvée"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public JobOfferResponse get(@PathVariable UUID id) {
        return JobOfferDtoMapper.toResponse(getJobOffer.execute(new JobOfferId(id)));
    }

    @GetMapping
    @Operation(summary = "Rechercher des offres (filtres, tri, pagination)", responses = {
            @ApiResponse(responseCode = "200", description = "Page de résultats"),
            @ApiResponse(responseCode = "400", description = "Paramètre invalide",
                    content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<JobOfferResponse> search(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) JobOfferStatus status,
            @Parameter(description = "Index de page (à partir de 0)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Taille de page (1 à " + PageRequest.MAX_SIZE + ")")
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageRequest.MAX_SIZE) int size,
            @Parameter(description = "<createdAt|updatedAt|publishedAt>[,asc|desc]", example = "createdAt,desc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        SortParameter sortParameter = SortParameter.parse(sort);
        JobOfferSearchCriteria criteria = new JobOfferSearchCriteria(
                companyId == null ? null : new CompanyId(companyId), status);
        PageRequest pageRequest = new PageRequest(page, size, sortParameter.field(), sortParameter.direction());
        return JobOfferDtoMapper.toResponse(searchJobOffers.execute(criteria, pageRequest));
    }
}
