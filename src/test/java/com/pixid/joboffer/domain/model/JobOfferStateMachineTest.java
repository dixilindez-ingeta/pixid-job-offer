package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.exception.DomainValidationException;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOfferStateMachineTest {

    @Test
    @DisplayName("Chemin nominal : tous les flags activés (Finalization -> Approval -> PostReview -> Published)")
    void nominalPathAllFlagsEnabled() {
        CompanyConfig config = new CompanyConfig(true, true, true);

        JobOfferStatus next1 = JobOfferStateMachine.getNextStatus(JobOfferStatus.DRAFT, config);
        assertThat(next1).isEqualTo(JobOfferStatus.TO_FINALIZE);

        JobOfferStatus next2 = JobOfferStateMachine.getNextStatus(next1, config);
        assertThat(next2).isEqualTo(JobOfferStatus.TO_APPROVE);

        JobOfferStatus next3 = JobOfferStateMachine.getNextStatus(next2, config);
        assertThat(next3).isEqualTo(JobOfferStatus.TO_POST);

        JobOfferStatus next4 = JobOfferStateMachine.getNextStatus(next3, config);
        assertThat(next4).isEqualTo(JobOfferStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Flag individuel : Finalization désactivé, les autres activés")
    void disableFinalizationOnly() {
        CompanyConfig config = new CompanyConfig(false, true, true);

        JobOfferStatus next = JobOfferStateMachine.getNextStatus(JobOfferStatus.DRAFT, config);
        assertThat(next).isEqualTo(JobOfferStatus.TO_APPROVE);
    }

    @Test
    @DisplayName("Flag individuel : Approval désactivé, les autres activés")
    void disableApprovalOnly() {
        CompanyConfig config = new CompanyConfig(true, false, true);

        JobOfferStatus nextFromFinalize = JobOfferStateMachine.getNextStatus(JobOfferStatus.TO_FINALIZE, config);
        assertThat(nextFromFinalize).isEqualTo(JobOfferStatus.TO_POST);
    }

    @Test
    @DisplayName("Flag individuel : PostReview désactivé, les autres activés")
    void disablePostReviewOnly() {
        CompanyConfig config = new CompanyConfig(true, true, false);

        JobOfferStatus nextFromApprove = JobOfferStateMachine.getNextStatus(JobOfferStatus.TO_APPROVE, config);
        assertThat(nextFromApprove).isEqualTo(JobOfferStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Combinaison : Tous les flags optionnels désactivés (DRAFT -> PUBLISHED direct)")
    void allFlagsDisabledDirectPublish() {
        CompanyConfig config = new CompanyConfig(false, false, false);

        JobOfferStatus next = JobOfferStateMachine.getNextStatus(JobOfferStatus.DRAFT, config);
        assertThat(next).isEqualTo(JobOfferStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Transition interdite : Tentative d'avancer depuis PUBLISHED ou CLOSED")
    void forbiddenTransitionsFromTerminalStates() {
        CompanyConfig config = new CompanyConfig(true, true, true);

        assertThatThrownBy(() -> JobOfferStateMachine.getNextStatus(JobOfferStatus.PUBLISHED, config))
                .isInstanceOf(DomainValidationException.class);

        assertThatThrownBy(() -> JobOfferStateMachine.getNextStatus(JobOfferStatus.CLOSED, config))
                .isInstanceOf(DomainValidationException.class);
    }
}