package com.pixid.joboffer.infrastructure.persistence.mapper;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.valueobject.*;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobOfferMapperTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    @DisplayName("Aller-retour d'une offre complète avec adresse personnalisée")
    void roundTripCompleteOffer() {
        Address address = new Address(List.of("1 rue de la Paix", "Bât. B"), "75002", "Paris", "IDF", "fr");
        Compensation compensation = new Compensation(
                List.of(new SalaryEntry(SalaryType.HOURLY, new BigDecimal("15.50"), "EUR")),
                List.of(new BonusEntry(BonusType.SIGNING, new BigDecimal("500"), "EUR")));
        JobOffer offer = JobOffer.rehydrate(JobOfferId.generate(), new CompanyId(UUID.randomUUID()), "Cariste",
                new Location(LocationType.CUSTOM, address), compensation,
                JobOfferStatus.PUBLISHED, T0, T0, T0, 2L);

        JobOfferJpaEntity entity = JobOfferMapper.toEntity(offer);
        JobOffer restored = JobOfferMapper.toDomain(entity);

        assertThat(entity.getVersion()).isEqualTo(2L);
        assertThat(entity.getSalaryEntries()).allSatisfy(s -> assertThat(s.getJobOffer()).isSameAs(entity));
        assertThat(restored).usingRecursiveComparison().isEqualTo(offer);
    }

    @Test
    @DisplayName("Aller-retour d'un brouillon partiel (location et compensation absentes)")
    void roundTripPartialDraft() {
        JobOffer offer = JobOffer.rehydrate(JobOfferId.generate(), new CompanyId(UUID.randomUUID()), null,
                null, null, JobOfferStatus.DRAFT, T0, T0, null, null);

        JobOfferJpaEntity entity = JobOfferMapper.toEntity(offer);
        JobOffer restored = JobOfferMapper.toDomain(entity);

        assertThat(entity.getLocationType()).isNull();
        assertThat(entity.getSalaryEntries()).isEmpty();
        assertThat(entity.getBonusEntries()).isEmpty();
        assertThat(restored).usingRecursiveComparison().isEqualTo(offer);
    }

    @Test
    @DisplayName("Location COMPANY_ADDRESS sans adresse")
    void companyAddressLocation() {
        JobOffer offer = JobOffer.rehydrate(JobOfferId.generate(), new CompanyId(UUID.randomUUID()), "Titre",
                new Location(LocationType.COMPANY_ADDRESS, null), null, JobOfferStatus.DRAFT, T0, T0, null, 0L);

        JobOffer restored = JobOfferMapper.toDomain(JobOfferMapper.toEntity(offer));

        assertThat(restored.getLocation()).isEqualTo(new Location(LocationType.COMPANY_ADDRESS, null));
    }
}
