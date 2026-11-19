package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.error.DomainValidationException;
import com.pixid.joboffer.domain.error.InvalidStateTransitionException;
import com.pixid.joboffer.domain.error.JobOfferModificationNotAllowedException;
import com.pixid.joboffer.domain.error.ResourceNotFoundException;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.JobOfferSearchCriteria;
import com.pixid.joboffer.domain.port.PageRequest;
import com.pixid.joboffer.domain.port.PageResult;
import com.pixid.joboffer.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.pixid.joboffer.domain.valueobject.JobOfferStatus.*;
import static com.pixid.joboffer.domain.valueobject.TransitionAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOfferUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-03-01T08:00:00Z");
    private static final Location LOCATION = new Location(LocationType.COMPANY_ADDRESS, null);
    private static final Compensation COMPENSATION = new Compensation(
            List.of(new SalaryEntry(SalaryType.HOURLY, new BigDecimal("14"), "EUR")), List.of());

    private final CompanyId companyId = CompanyId.generate();
    private InMemoryJobOfferRepository repository;
    private InMemoryCompanyConfigProvider configs;
    private CreateJobOffer createJobOffer;
    private UpdateJobOffer updateJobOffer;
    private TransitionJobOffer transitionJobOffer;
    private GetJobOffer getJobOffer;
    private SearchJobOffers searchJobOffers;

    @BeforeEach
    void setUp() {
        repository = new InMemoryJobOfferRepository();
        configs = new InMemoryCompanyConfigProvider();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        createJobOffer = new CreateJobOffer(repository, configs, clock);
        updateJobOffer = new UpdateJobOffer(repository, configs, clock);
        transitionJobOffer = new TransitionJobOffer(repository, configs, clock);
        getJobOffer = new GetJobOffer(repository, configs);
        searchJobOffers = new SearchJobOffers(repository);
    }

    private JobOfferDetails createComplete() {
        return createJobOffer.execute(new CreateJobOfferCommand(companyId, "Préparateur de commandes", LOCATION, COMPENSATION));
    }

    @Test
    @DisplayName("Création : brouillon persisté avec actions disponibles et dates de l'horloge")
    void createPersistsDraft() {
        JobOfferDetails details = createComplete();

        JobOffer offer = details.jobOffer();
        assertThat(offer.getStatus()).isEqualTo(DRAFT);
        assertThat(offer.getCreatedAt()).isEqualTo(NOW);
        assertThat(offer.getVersion()).isZero();
        assertThat(details.availableActions()).containsExactly(SUBMIT);
        assertThat(repository.findById(offer.getId())).isPresent();
    }

    @Test
    @DisplayName("Création partielle : dépend de la configuration de l'entreprise")
    void partialCreationDependsOnCompanyConfig() {
        CreateJobOfferCommand partial = new CreateJobOfferCommand(companyId, "Titre seul", null, null);

        assertThatThrownBy(() -> createJobOffer.execute(partial)).isInstanceOf(DomainValidationException.class);
        assertThat(repository.saveCount).isZero();

        configs.save(companyId, new CompanyConfig(false, true, false));
        assertThat(createJobOffer.execute(partial).jobOffer().isComplete()).isFalse();
    }

    @Test
    @DisplayName("Mise à jour : champs fournis modifiés, version incrémentée")
    void updateModifiesDraft() {
        JobOfferId id = createComplete().jobOffer().getId();

        JobOfferDetails updated = updateJobOffer.execute(new UpdateJobOfferCommand(id, "Cariste", null, null));

        assertThat(updated.jobOffer().getTitle()).isEqualTo("Cariste");
        assertThat(updated.jobOffer().getCompensation()).isEqualTo(COMPENSATION);
        assertThat(updated.jobOffer().getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Mise à jour d'une offre publiée refusée")
    void updatePublishedOfferRejected() {
        JobOfferId id = createComplete().jobOffer().getId();
        transitionJobOffer.execute(id, SUBMIT);

        assertThatThrownBy(() -> updateJobOffer.execute(new UpdateJobOfferCommand(id, "Cariste", null, null)))
                .isInstanceOf(JobOfferModificationNotAllowedException.class);
    }

    @Test
    @DisplayName("Transition : utilise la configuration de l'entreprise de l'offre")
    void transitionUsesCompanyConfig() {
        configs.save(companyId, new CompanyConfig(true, false, false));
        JobOfferId id = createComplete().jobOffer().getId();

        JobOfferDetails submitted = transitionJobOffer.execute(id, SUBMIT);
        assertThat(submitted.jobOffer().getStatus()).isEqualTo(TO_APPROVE);
        assertThat(submitted.availableActions()).containsExactlyInAnyOrder(APPROVE, REJECT);

        JobOfferDetails approved = transitionJobOffer.execute(id, APPROVE);
        assertThat(approved.jobOffer().getStatus()).isEqualTo(PUBLISHED);
        assertThat(approved.jobOffer().getPublishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Transition interdite : rien n'est persisté")
    void forbiddenTransitionIsNotPersisted() {
        JobOfferId id = createComplete().jobOffer().getId();
        int savesBefore = repository.saveCount;

        assertThatThrownBy(() -> transitionJobOffer.execute(id, CLOSE))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(repository.saveCount).isEqualTo(savesBefore);
    }

    @Test
    @DisplayName("Lecture : actions calculées avec la configuration courante")
    void getComputesAvailableActions() {
        JobOfferId id = createComplete().jobOffer().getId();

        assertThat(getJobOffer.execute(id).availableActions()).containsExactly(SUBMIT);
    }

    @Test
    @DisplayName("Offre inexistante : ResourceNotFoundException pour lecture, mise à jour et transition")
    void unknownOfferNotFound() {
        JobOfferId unknown = JobOfferId.generate();

        assertThatThrownBy(() -> getJobOffer.execute(unknown))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknown.value().toString());
        assertThatThrownBy(() -> updateJobOffer.execute(new UpdateJobOfferCommand(unknown, "T", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> transitionJobOffer.execute(unknown, SUBMIT))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Recherche : délègue au port avec critères et pagination")
    void searchDelegatesToRepository() {
        createComplete();
        createComplete();
        createJobOffer.execute(new CreateJobOfferCommand(CompanyId.generate(), "Autre", LOCATION, COMPENSATION));

        PageResult<JobOffer> page = searchJobOffers.execute(new JobOfferSearchCriteria(companyId, DRAFT),
                new PageRequest(0, 1, PageRequest.SortField.CREATED_AT, PageRequest.SortDirection.DESC));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
