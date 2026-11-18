package com.pixid.joboffer.domain.exception;

import com.pixid.joboffer.domain.valueobject.JobOfferStatus;
import com.pixid.joboffer.domain.valueobject.TransitionAction;

public class InvalidStateTransitionException extends DomainException {
    private static final long serialVersionUID = 1L;

    public InvalidStateTransitionException(JobOfferStatus currentStatus, TransitionAction action) {
        super("Transition '" + action + "' is not allowed from status '" + currentStatus + "'");
    }
}