package com.pixid.joboffer.domain.service;

import com.pixid.joboffer.domain.error.InvalidStateTransitionException;
import com.pixid.joboffer.domain.model.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.domain.valueobject.TransitionAction;

import java.util.EnumSet;
import java.util.Set;

public final class JobOfferStateMachine {

    private JobOfferStateMachine() {}

    public static JobOfferStatus computeNextStatus(
            JobOfferStatus current,
            TransitionAction action,
            CompanyConfig config
    ) {
        if (!getAvailableTransitions(current, config).contains(action)) {
            throw new InvalidStateTransitionException(current, action);
        }

        return switch (current) {
            case DRAFT -> switch (action) {
                case SUBMIT -> config.partialSaveEnabled()
                        ? JobOfferStatus.TO_FINALIZE
                        : resolveAfterFinalization(config);
                default -> throw new InvalidStateTransitionException(current, action);
            };
            case TO_FINALIZE -> switch (action) {
                case FINALIZE -> resolveAfterFinalization(config);
                default -> throw new InvalidStateTransitionException(current, action);
            };
            case TO_APPROVE -> switch (action) {
                case APPROVE -> resolveAfterApproval(config);
                case REJECT -> JobOfferStatus.DRAFT; // Retour en brouillon pour correction suite au rejet
                default -> throw new InvalidStateTransitionException(current, action);
            };
            case TO_POST -> switch (action) {
                case POST -> JobOfferStatus.PUBLISHED;
                default -> throw new InvalidStateTransitionException(current, action);
            };
            case PUBLISHED -> switch (action) {
                case CLOSE, EXPIRE -> JobOfferStatus.CLOSED;
                default -> throw new InvalidStateTransitionException(current, action);
            };
            case CLOSED -> throw new InvalidStateTransitionException(current, action);
        };
    }

    public static Set<TransitionAction> getAvailableTransitions(JobOfferStatus current, CompanyConfig config) {
        return switch (current) {
            case DRAFT -> EnumSet.of(TransitionAction.SUBMIT);
            case TO_FINALIZE -> EnumSet.of(TransitionAction.FINALIZE);
            case TO_APPROVE -> EnumSet.of(TransitionAction.APPROVE, TransitionAction.REJECT);
            case TO_POST -> EnumSet.of(TransitionAction.POST);
            case PUBLISHED -> EnumSet.of(TransitionAction.CLOSE, TransitionAction.EXPIRE);
            case CLOSED -> EnumSet.noneOf(TransitionAction.class);
        };
    }

    private static JobOfferStatus resolveAfterFinalization(CompanyConfig config) {
        if (config.approvalRequired()) {
            return JobOfferStatus.TO_APPROVE;
        }
        return resolveAfterApproval(config);
    }

    private static JobOfferStatus resolveAfterApproval(CompanyConfig config) {
        if (config.manualPostingRequired()) {
            return JobOfferStatus.TO_POST;
        }
        return JobOfferStatus.PUBLISHED;
    }
}