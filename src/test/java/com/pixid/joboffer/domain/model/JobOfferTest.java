package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.error.DomainValidationException;
import com.pixid.joboffer.domain.error.IncompleteJobOfferException;
import com.pixid.joboffer.domain.error.InvalidStateTransitionException;
import com.pixid.joboffer.domain.error.JobOfferModificationNotAllowedException;
import com.pixid.joboffer.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.pixid.joboffer.domain.valueobject.JobOfferStatus.*;
import static com.pixid.joboffer.domain.valueobject.TransitionAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOfferTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T10:00:00Z");
    private static final Clock CLOCK_T0 = Clock.fixed(T0, ZoneOffset.UTC);
    private static final Clock CLOCK_T1 = Clock.fixed(T1, ZoneOffset.UTC);

    private static final CompanyConfig ALL_ENABLED = new CompanyConfig(true, true, true);
    private static final CompanyConfig ALL_DISABLED = CompanyConfig.defaults();
    private static final CompanyConfig PARTIAL_ONLY = new CompanyConfig(false, true, false);

    private static final Location LOCATION = new Location(LocationType.COMPANY_ADDRESS, null);
    private static final Compensation COMPENSATION = new Compensation(
            List.of(new SalaryEntry(SalaryType.MONTHLY, new BigDecimal("2500"), "EUR")), List.of());

    private static JobOffer completeDraft(CompanyConfig config) {
        return JobOffer.createDraft(JobOfferId.generate(), CompanyId.generate(),
                "Développeur Java", LOCATION, COMPENSATION, config, CLOCK_T0);
    }

    private static JobOffer emptyDraft() {
        return JobOffer.createDraft(JobOfferId.generate(), CompanyId.generate(),
                null, null, null, PARTIAL_ONLY, CLOCK_T0);
    }

    private static JobOffer inStatus(JobOfferStatus status) {
        return JobOffer.rehydrate(JobOfferId.generate(), CompanyId.generate(), "Titre",
                LOCATION, COMPENSATION, status, T0, T0, null, 3L);
    }

    @Test
    @DisplayName("rehydrate restaure l'état tel quel, sans validation")
    void rehydrateRestoresStateWithoutValidation() {
        JobOfferId id = JobOfferId.generate();
        CompanyId companyId = CompanyId.generate();

        JobOffer offer = JobOffer.rehydrate(id, companyId, null, null, null, PUBLISHED, T0, T1, T1, 7L);

        assertThat(offer.getId()).isEqualTo(id);
        assertThat(offer.getCompanyId()).isEqualTo(companyId);
        assertThat(offer.getStatus()).isEqualTo(PUBLISHED);
        assertThat(offer.getCreatedAt()).isEqualTo(T0);
        assertThat(offer.getUpdatedAt()).isEqualTo(T1);
        assertThat(offer.getPublishedAt()).isEqualTo(T1);
        assertThat(offer.getVersion()).isEqualTo(7L);
    }

    @Nested
    class Creation {

        @Test
        @DisplayName("Un brouillon complet est créé en DRAFT avec les dates de l'horloge")
        void createsCompleteDraft() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            assertThat(offer.getStatus()).isEqualTo(DRAFT);
            assertThat(offer.getCreatedAt()).isEqualTo(T0);
            assertThat(offer.getUpdatedAt()).isEqualTo(T0);
            assertThat(offer.getPublishedAt()).isNull();
            assertThat(offer.getVersion()).isNull();
            assertThat(offer.isComplete()).isTrue();
        }

        @Test
        @DisplayName("Données partielles autorisées si partialSaveEnabled")
        void partialDraftAllowedWhenPartialSaveEnabled() {
            JobOffer offer = emptyDraft();

            assertThat(offer.getStatus()).isEqualTo(DRAFT);
            assertThat(offer.isComplete()).isFalse();
        }

        @Test
        @DisplayName("Données partielles refusées si partialSaveEnabled est désactivé")
        void partialDraftRejectedWhenPartialSaveDisabled() {
            assertThatThrownBy(() -> JobOffer.createDraft(JobOfferId.generate(), CompanyId.generate(),
                    "Titre", null, null, ALL_DISABLED, CLOCK_T0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("location")
                    .hasMessageContaining("compensation");
        }

        @Test
        @DisplayName("Un titre vide compte comme manquant")
        void blankTitleIsMissing() {
            assertThatThrownBy(() -> JobOffer.createDraft(JobOfferId.generate(), CompanyId.generate(),
                    "  ", LOCATION, COMPENSATION, ALL_DISABLED, CLOCK_T0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("title");
        }

        @Test
        @DisplayName("Le titre est limité à 200 caractères, même en enregistrement partiel")
        void titleMaxLength() {
            assertThatThrownBy(() -> JobOffer.createDraft(JobOfferId.generate(), CompanyId.generate(),
                    "x".repeat(201), null, null, PARTIAL_ONLY, CLOCK_T0))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Identifiants obligatoires")
        void idsAreRequired() {
            assertThatThrownBy(() -> JobOffer.createDraft(null, CompanyId.generate(),
                    null, null, null, PARTIAL_ONLY, CLOCK_T0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> JobOffer.createDraft(JobOfferId.generate(), null,
                    null, null, null, PARTIAL_ONLY, CLOCK_T0))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class Update {

        @Test
        @DisplayName("Mise à jour partielle : les champs null sont conservés")
        void partialUpdateKeepsNullFields() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            offer.update("Nouveau titre", null, null, CLOCK_T1);

            assertThat(offer.getTitle()).isEqualTo("Nouveau titre");
            assertThat(offer.getLocation()).isEqualTo(LOCATION);
            assertThat(offer.getCompensation()).isEqualTo(COMPENSATION);
            assertThat(offer.getUpdatedAt()).isEqualTo(T1);
            assertThat(offer.getCreatedAt()).isEqualTo(T0);
        }

        @Test
        @DisplayName("Mise à jour autorisée en TO_FINALIZE")
        void updateAllowedInToFinalize() {
            JobOffer offer = inStatus(TO_FINALIZE);

            offer.update("Nouveau titre", null, null, CLOCK_T1);

            assertThat(offer.getTitle()).isEqualTo("Nouveau titre");
        }

        @Test
        @DisplayName("Un titre vide est refusé")
        void blankTitleRejected() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            assertThatThrownBy(() -> offer.update(" ", null, null, CLOCK_T1))
                    .isInstanceOf(DomainValidationException.class);
        }

        @ParameterizedTest
        @EnumSource(value = JobOfferStatus.class, names = {"TO_APPROVE", "TO_POST", "PUBLISHED", "CLOSED"})
        @DisplayName("Mise à jour interdite hors DRAFT / TO_FINALIZE")
        void updateForbiddenOutsideEditableStatuses(JobOfferStatus status) {
            JobOffer offer = inStatus(status);

            assertThatThrownBy(() -> offer.update("Nouveau titre", null, null, CLOCK_T1))
                    .isInstanceOf(JobOfferModificationNotAllowedException.class)
                    .hasMessageContaining(status.name());
            assertThat(offer.getTitle()).isEqualTo("Titre");
        }
    }

    @Nested
    class Transitions {

        @Test
        @DisplayName("Parcours complet avec toutes les étapes activées")
        void fullWorkflowAllEnabled() {
            JobOffer offer = completeDraft(ALL_ENABLED);

            offer.apply(SUBMIT, ALL_ENABLED, CLOCK_T0);
            assertThat(offer.getStatus()).isEqualTo(TO_FINALIZE);
            offer.apply(FINALIZE, ALL_ENABLED, CLOCK_T0);
            assertThat(offer.getStatus()).isEqualTo(TO_APPROVE);
            offer.apply(APPROVE, ALL_ENABLED, CLOCK_T0);
            assertThat(offer.getStatus()).isEqualTo(TO_POST);
            assertThat(offer.getPublishedAt()).isNull();
            offer.apply(POST, ALL_ENABLED, CLOCK_T1);
            assertThat(offer.getStatus()).isEqualTo(PUBLISHED);
            assertThat(offer.getPublishedAt()).isEqualTo(T1);
            offer.apply(CLOSE, ALL_ENABLED, CLOCK_T1);
            assertThat(offer.getStatus()).isEqualTo(CLOSED);
        }

        @Test
        @DisplayName("Toutes les étapes désactivées : SUBMIT publie directement et renseigne publishedAt")
        void submitPublishesDirectlyWhenAllDisabled() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            offer.apply(SUBMIT, ALL_DISABLED, CLOCK_T1);

            assertThat(offer.getStatus()).isEqualTo(PUBLISHED);
            assertThat(offer.getPublishedAt()).isEqualTo(T1);
            assertThat(offer.getUpdatedAt()).isEqualTo(T1);
        }

        @Test
        @DisplayName("Avec partialSaveEnabled, SUBMIT accepte une offre incomplète (-> TO_FINALIZE)")
        void submitIncompleteAllowedWithPartialSave() {
            JobOffer offer = emptyDraft();

            offer.apply(SUBMIT, PARTIAL_ONLY, CLOCK_T1);

            assertThat(offer.getStatus()).isEqualTo(TO_FINALIZE);
        }

        @Test
        @DisplayName("FINALIZE exige une offre complète")
        void finalizeRequiresCompleteOffer() {
            JobOffer offer = emptyDraft();
            offer.apply(SUBMIT, PARTIAL_ONLY, CLOCK_T0);

            assertThatThrownBy(() -> offer.apply(FINALIZE, PARTIAL_ONLY, CLOCK_T1))
                    .isInstanceOfSatisfying(IncompleteJobOfferException.class, e -> assertThat(e.getMissingFields())
                            .containsExactly("title", "location", "compensation"));
            assertThat(offer.getStatus()).isEqualTo(TO_FINALIZE);

            offer.update("Titre", LOCATION, COMPENSATION, CLOCK_T1);
            offer.apply(FINALIZE, PARTIAL_ONLY, CLOCK_T1);
            assertThat(offer.getStatus()).isEqualTo(PUBLISHED);
        }

        @Test
        @DisplayName("Sans partialSaveEnabled, SUBMIT exige une offre complète")
        void submitRequiresCompleteOfferWithoutPartialSave() {
            JobOffer offer = emptyDraft();

            assertThatThrownBy(() -> offer.apply(SUBMIT, ALL_DISABLED, CLOCK_T1))
                    .isInstanceOf(IncompleteJobOfferException.class);
            assertThat(offer.getStatus()).isEqualTo(DRAFT);
        }

        @Test
        @DisplayName("REJECT renvoie en DRAFT, l'offre redevient modifiable et peut être resoumise")
        void rejectGoesBackToDraft() {
            JobOffer offer = inStatus(TO_APPROVE);

            offer.apply(REJECT, ALL_ENABLED, CLOCK_T1);

            assertThat(offer.getStatus()).isEqualTo(DRAFT);
            assertThat(offer.isModifiable()).isTrue();
            assertThat(offer.getPublishedAt()).isNull();
            offer.apply(SUBMIT, ALL_ENABLED, CLOCK_T1);
            assertThat(offer.getStatus()).isEqualTo(TO_FINALIZE);
        }

        @Test
        @DisplayName("EXPIRE ferme une offre publiée")
        void expireClosesPublishedOffer() {
            JobOffer offer = inStatus(PUBLISHED);

            offer.apply(EXPIRE, ALL_DISABLED, CLOCK_T1);

            assertThat(offer.getStatus()).isEqualTo(CLOSED);
        }

        @Test
        @DisplayName("CLOSE interdit hors PUBLISHED")
        void closeOnlyFromPublished() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            assertThatThrownBy(() -> offer.apply(CLOSE, ALL_DISABLED, CLOCK_T1))
                    .isInstanceOf(InvalidStateTransitionException.class);
            assertThat(offer.getStatus()).isEqualTo(DRAFT);
            assertThat(offer.getUpdatedAt()).isEqualTo(T0);
        }

        @Test
        @DisplayName("Aucune action depuis CLOSED")
        void noActionFromClosed() {
            JobOffer offer = inStatus(CLOSED);

            assertThatThrownBy(() -> offer.apply(SUBMIT, ALL_DISABLED, CLOCK_T1))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        @DisplayName("Une action nulle est refusée")
        void nullActionRejected() {
            JobOffer offer = completeDraft(ALL_DISABLED);

            assertThatThrownBy(() -> offer.apply(null, ALL_DISABLED, CLOCK_T1))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    class AvailableActions {

        @Test
        @DisplayName("Par statut, pour une offre complète")
        void perStatus() {
            assertThat(completeDraft(ALL_DISABLED).availableActions(ALL_DISABLED)).containsExactly(SUBMIT);
            assertThat(inStatus(TO_FINALIZE).availableActions(ALL_ENABLED)).containsExactly(FINALIZE);
            assertThat(inStatus(TO_APPROVE).availableActions(ALL_ENABLED)).containsExactlyInAnyOrder(APPROVE, REJECT);
            assertThat(inStatus(TO_POST).availableActions(ALL_ENABLED)).containsExactly(POST);
            assertThat(inStatus(PUBLISHED).availableActions(ALL_ENABLED)).containsExactlyInAnyOrder(CLOSE, EXPIRE);
            assertThat(inStatus(CLOSED).availableActions(ALL_ENABLED)).isEmpty();
        }

        @Test
        @DisplayName("Offre incomplète : SUBMIT disponible seulement si partialSaveEnabled")
        void incompleteDraftDependsOnConfig() {
            JobOffer offer = emptyDraft();

            assertThat(offer.availableActions(PARTIAL_ONLY)).containsExactly(SUBMIT);
            assertThat(offer.availableActions(ALL_DISABLED)).isEmpty();
        }

        @Test
        @DisplayName("Offre incomplète en TO_FINALIZE : FINALIZE indisponible")
        void incompleteOfferCannotBeFinalized() {
            JobOffer offer = emptyDraft();
            offer.apply(SUBMIT, PARTIAL_ONLY, CLOCK_T0);

            assertThat(offer.availableActions(PARTIAL_ONLY)).isEmpty();
        }
    }
}
