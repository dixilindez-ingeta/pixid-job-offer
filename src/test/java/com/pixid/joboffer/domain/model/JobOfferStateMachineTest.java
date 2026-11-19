package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.error.InvalidStateTransitionException;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.domain.valueobject.TransitionAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Set;

import static com.pixid.joboffer.domain.valueobject.JobOfferStatus.*;
import static com.pixid.joboffer.domain.valueobject.TransitionAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOfferStateMachineTest {

    private static final CompanyConfig ALL_ENABLED = new CompanyConfig(true, true, true);
    private static final CompanyConfig ALL_DISABLED = CompanyConfig.defaults();

    @Test
    @DisplayName("Chemin nominal : tous les flags activés (DRAFT -> TO_FINALIZE -> TO_APPROVE -> TO_POST -> PUBLISHED -> CLOSED)")
    void nominalPathAllFlagsEnabled() {
        assertThat(JobOfferStateMachine.targetStatus(DRAFT, SUBMIT, ALL_ENABLED)).isEqualTo(TO_FINALIZE);
        assertThat(JobOfferStateMachine.targetStatus(TO_FINALIZE, FINALIZE, ALL_ENABLED)).isEqualTo(TO_APPROVE);
        assertThat(JobOfferStateMachine.targetStatus(TO_APPROVE, APPROVE, ALL_ENABLED)).isEqualTo(TO_POST);
        assertThat(JobOfferStateMachine.targetStatus(TO_POST, POST, ALL_ENABLED)).isEqualTo(PUBLISHED);
        assertThat(JobOfferStateMachine.targetStatus(PUBLISHED, CLOSE, ALL_ENABLED)).isEqualTo(CLOSED);
    }

    @Test
    @DisplayName("Tous les flags désactivés : SUBMIT publie directement")
    void allFlagsDisabledDirectPublish() {
        assertThat(JobOfferStateMachine.targetStatus(DRAFT, SUBMIT, ALL_DISABLED)).isEqualTo(PUBLISHED);
    }

    @ParameterizedTest(name = "approval={0}, partialSave={1}, manualPosting={2} : SUBMIT -> {3}")
    @CsvSource({
            "false, true,  false, TO_FINALIZE",
            "true,  false, false, TO_APPROVE",
            "false, false, true,  TO_POST",
            "true,  false, true,  TO_APPROVE",
            "false, false, false, PUBLISHED"
    })
    @DisplayName("SUBMIT saute les états désactivés")
    void submitSkipsDisabledStates(boolean approval, boolean partialSave, boolean manualPosting, JobOfferStatus expected) {
        CompanyConfig config = new CompanyConfig(approval, partialSave, manualPosting);
        assertThat(JobOfferStateMachine.targetStatus(DRAFT, SUBMIT, config)).isEqualTo(expected);
    }

    @Test
    @DisplayName("FINALIZE et APPROVE sautent les états désactivés")
    void forwardActionsSkipDisabledStates() {
        CompanyConfig noApproval = new CompanyConfig(false, true, true);
        assertThat(JobOfferStateMachine.targetStatus(TO_FINALIZE, FINALIZE, noApproval)).isEqualTo(TO_POST);

        CompanyConfig noManualPosting = new CompanyConfig(true, true, false);
        assertThat(JobOfferStateMachine.targetStatus(TO_FINALIZE, FINALIZE, noManualPosting)).isEqualTo(TO_APPROVE);
        assertThat(JobOfferStateMachine.targetStatus(TO_APPROVE, APPROVE, noManualPosting)).isEqualTo(PUBLISHED);
    }

    @Test
    @DisplayName("REJECT renvoie en DRAFT")
    void rejectGoesBackToDraft() {
        assertThat(JobOfferStateMachine.targetStatus(TO_APPROVE, REJECT, ALL_ENABLED)).isEqualTo(DRAFT);
    }

    @Test
    @DisplayName("CLOSE et EXPIRE mènent à CLOSED depuis PUBLISHED")
    void closeAndExpireFromPublished() {
        assertThat(JobOfferStateMachine.targetStatus(PUBLISHED, CLOSE, ALL_DISABLED)).isEqualTo(CLOSED);
        assertThat(JobOfferStateMachine.targetStatus(PUBLISHED, EXPIRE, ALL_DISABLED)).isEqualTo(CLOSED);
    }

    @Test
    @DisplayName("Actions autorisées par statut")
    void allowedActionsPerStatus() {
        assertThat(JobOfferStateMachine.allowedActions(DRAFT)).containsExactly(SUBMIT);
        assertThat(JobOfferStateMachine.allowedActions(TO_FINALIZE)).containsExactly(FINALIZE);
        assertThat(JobOfferStateMachine.allowedActions(TO_APPROVE)).containsExactlyInAnyOrder(APPROVE, REJECT);
        assertThat(JobOfferStateMachine.allowedActions(TO_POST)).containsExactly(POST);
        assertThat(JobOfferStateMachine.allowedActions(PUBLISHED)).containsExactlyInAnyOrder(CLOSE, EXPIRE);
        assertThat(JobOfferStateMachine.allowedActions(CLOSED)).isEmpty();
    }

    @ParameterizedTest(name = "{0} : actions interdites rejetées")
    @EnumSource(JobOfferStatus.class)
    @DisplayName("Toute action non autorisée lève InvalidStateTransitionException")
    void forbiddenActionsAreRejected(JobOfferStatus status) {
        Set<TransitionAction> forbidden = EnumSet.allOf(TransitionAction.class);
        forbidden.removeAll(JobOfferStateMachine.allowedActions(status));

        for (TransitionAction action : forbidden) {
            assertThatThrownBy(() -> JobOfferStateMachine.targetStatus(status, action, ALL_ENABLED))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(action.name())
                    .hasMessageContaining(status.name());
        }
    }
}
