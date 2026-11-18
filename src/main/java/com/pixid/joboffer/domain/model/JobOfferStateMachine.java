package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.exception.InvalidStateTransitionException;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.domain.valueobject.TransitionAction;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static com.pixid.joboffer.domain.valueobject.TransitionAction.*;

public final class JobOfferStateMachine {

    private JobOfferStateMachine() {}

    public static Set<TransitionAction> allowedActions(JobOfferStatus status) {
        return Collections.unmodifiableSet(switch (status) {
            case DRAFT -> EnumSet.of(SUBMIT);
            case TO_FINALIZE -> EnumSet.of(FINALIZE);
            case TO_APPROVE -> EnumSet.of(APPROVE, REJECT);
            case TO_POST -> EnumSet.of(POST);
            case PUBLISHED -> EnumSet.of(CLOSE, EXPIRE);
            case CLOSED -> EnumSet.noneOf(TransitionAction.class);
        });
    }

    public static JobOfferStatus targetStatus(JobOfferStatus status, TransitionAction action, CompanyConfig config) {
        if (!allowedActions(status).contains(action)) {
            throw new InvalidStateTransitionException(status, action);
        }
        return switch (action) {
            case SUBMIT -> config.partialSaveEnabled() ? JobOfferStatus.TO_FINALIZE : nextAfterFinalize(config);
            case FINALIZE -> nextAfterFinalize(config);
            case APPROVE -> nextAfterApprove(config);
            case REJECT -> JobOfferStatus.DRAFT;
            case POST -> JobOfferStatus.PUBLISHED;
            case CLOSE, EXPIRE -> JobOfferStatus.CLOSED;
        };
    }

    private static JobOfferStatus nextAfterFinalize(CompanyConfig config) {
        return config.approvalRequired() ? JobOfferStatus.TO_APPROVE : nextAfterApprove(config);
    }

    private static JobOfferStatus nextAfterApprove(CompanyConfig config) {
        return config.manualPostingRequired() ? JobOfferStatus.TO_POST : JobOfferStatus.PUBLISHED;
    }
}
