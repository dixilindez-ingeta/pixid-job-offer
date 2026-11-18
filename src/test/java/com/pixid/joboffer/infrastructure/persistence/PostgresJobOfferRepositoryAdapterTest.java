package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.TestcontainersConfiguration;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.*;
import com.pixid.joboffer.domain.port.PageRequest.SortDirection;
import com.pixid.joboffer.domain.port.PageRequest.SortField;
import com.pixid.joboffer.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PostgresJobOfferRepositoryAdapterTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
    private static final Location CUSTOM_LOCATION = new Location(LocationType.CUSTOM,
            new Address(List.of("10 avenue de France"), "75013", "Paris", null, "FR"));
    private static final Compensation COMPENSATION = new Compensation(
            List.of(new SalaryEntry(SalaryType.MONTHLY, new BigDecimal("2100.00"), "EUR")),
            List.of(new BonusEntry(BonusType.ANNUAL, new BigDecimal("1000.00"), "EUR")));

    @Autowired
    private JobOfferRepository repository;
    @Autowired
    private CompanyConfigProvider companyConfigProvider;
    @Autowired
    private SpringDataJobOfferRepository springDataRepository;
    @Autowired
    private SpringDataCompanyConfigRepository springDataCompanyConfigRepository;

    private static JobOffer draft(CompanyId companyId) {
        return JobOffer.createDraft(JobOfferId.generate(), companyId, "Magasinier",
                CUSTOM_LOCATION, COMPENSATION, CompanyConfig.defaults(), CLOCK);
    }

    private static JobOffer stored(CompanyId companyId, JobOfferStatus status, Instant createdAt) {
        return JobOffer.rehydrate(JobOfferId.generate(), companyId, "Offre " + createdAt, CUSTOM_LOCATION,
                COMPENSATION, status, createdAt, createdAt, null, null);
    }

    private static List<JobOfferId> ids(PageResult<JobOffer> page) {
        return page.content().stream().map(JobOffer::getId).toList();
    }

    @BeforeEach
    void cleanDatabase() {
        springDataRepository.deleteAll();
        springDataCompanyConfigRepository.deleteAll();
    }

    @Test
    @DisplayName("save puis findById : aller-retour complet, version initialisée")
    void saveAndFindRoundTrip() {
        JobOffer saved = repository.save(draft(new CompanyId(UUID.randomUUID())));

        JobOffer found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getVersion()).isZero();
        assertThat(found).usingRecursiveComparison().isEqualTo(saved);
    }

    @Test
    @DisplayName("Brouillon partiel persisté sans location ni compensation")
    void savePartialDraft() {
        JobOffer partial = JobOffer.createDraft(JobOfferId.generate(), new CompanyId(UUID.randomUUID()),
                null, null, null, new CompanyConfig(false, true, false), CLOCK);

        JobOffer found = repository.findById(repository.save(partial).getId()).orElseThrow();

        assertThat(found.getTitle()).isNull();
        assertThat(found.getLocation()).isNull();
        assertThat(found.getCompensation()).isNull();
    }

    @Test
    @DisplayName("Mise à jour : collections remplacées, version incrémentée")
    void updateReplacesChildrenAndIncrementsVersion() {
        JobOffer offer = repository.save(draft(new CompanyId(UUID.randomUUID())));
        Compensation newCompensation = new Compensation(
                List.of(new SalaryEntry(SalaryType.HOURLY, new BigDecimal("13.50"), "EUR"),
                        new SalaryEntry(SalaryType.DAILY, new BigDecimal("110.00"), "EUR")),
                List.of());

        offer.update("Magasinier cariste", new Location(LocationType.COMPANY_ADDRESS, null), newCompensation,
                Clock.offset(CLOCK, java.time.Duration.ofHours(1)));
        JobOffer updated = repository.save(offer);
        JobOffer found = repository.findById(offer.getId()).orElseThrow();

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(found.getTitle()).isEqualTo("Magasinier cariste");
        assertThat(found.getLocation().address()).isNull();
        assertThat(found.getCompensation().salaryEntries()).containsExactlyInAnyOrderElementsOf(newCompensation.salaryEntries());
        assertThat(found.getCompensation().bonusEntries()).isEmpty();
        assertThat(found.getUpdatedAt()).isEqualTo(T0.plus(1, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Mise à jour concurrente : OptimisticLockingFailureException")
    void concurrentUpdateFails() {
        JobOfferId id = repository.save(draft(new CompanyId(UUID.randomUUID()))).getId();
        JobOffer first = repository.findById(id).orElseThrow();
        JobOffer second = repository.findById(id).orElseThrow();

        first.update("Première version", null, null, CLOCK);
        repository.save(first);
        second.update("Seconde version", null, null, CLOCK);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(repository.findById(id).orElseThrow().getTitle()).isEqualTo("Première version");
    }

    @Test
    @DisplayName("Recherche : filtres companyId / status, tri et pagination")
    void searchFiltersSortsAndPaginates() {
        CompanyId companyA = new CompanyId(UUID.randomUUID());
        CompanyId companyB = new CompanyId(UUID.randomUUID());
        JobOffer a1 = repository.save(stored(companyA, JobOfferStatus.DRAFT, T0));
        JobOffer a2 = repository.save(stored(companyA, JobOfferStatus.DRAFT, T0.plusSeconds(10)));
        JobOffer a3 = repository.save(stored(companyA, JobOfferStatus.PUBLISHED, T0.plusSeconds(20)));
        JobOffer b1 = repository.save(stored(companyB, JobOfferStatus.DRAFT, T0.plusSeconds(30)));

        PageRequest firstPageDesc = new PageRequest(0, 2, SortField.CREATED_AT, SortDirection.DESC);

        PageResult<JobOffer> all = repository.search(new JobOfferSearchCriteria(null, null), firstPageDesc);
        assertThat(ids(all)).containsExactly(b1.getId(), a3.getId());
        assertThat(all.totalElements()).isEqualTo(4);
        assertThat(all.totalPages()).isEqualTo(2);

        PageResult<JobOffer> companyAOnly = repository.search(new JobOfferSearchCriteria(companyA, null),
                new PageRequest(1, 2, SortField.CREATED_AT, SortDirection.DESC));
        assertThat(ids(companyAOnly)).containsExactly(a1.getId());
        assertThat(companyAOnly.page()).isEqualTo(1);

        PageResult<JobOffer> drafts = repository.search(new JobOfferSearchCriteria(null, JobOfferStatus.DRAFT),
                new PageRequest(0, 10, SortField.CREATED_AT, SortDirection.ASC));
        assertThat(ids(drafts)).containsExactly(a1.getId(), a2.getId(), b1.getId());

        PageResult<JobOffer> companyADrafts = repository.search(
                new JobOfferSearchCriteria(companyA, JobOfferStatus.DRAFT), firstPageDesc);
        assertThat(ids(companyADrafts)).containsExactly(a2.getId(), a1.getId());
        assertThat(companyADrafts.content().getFirst().getCompensation()).isEqualTo(COMPENSATION);
    }

    @Test
    @DisplayName("Configuration : valeurs par défaut si absente, upsert ensuite")
    void companyConfigDefaultsAndUpsert() {
        CompanyId companyId = new CompanyId(UUID.randomUUID());
        assertThat(companyConfigProvider.findByCompanyId(companyId)).isEqualTo(CompanyConfig.defaults());

        companyConfigProvider.save(companyId, new CompanyConfig(true, true, false));
        companyConfigProvider.save(companyId, new CompanyConfig(true, false, true));

        assertThat(companyConfigProvider.findByCompanyId(companyId)).isEqualTo(new CompanyConfig(true, false, true));
    }
}
