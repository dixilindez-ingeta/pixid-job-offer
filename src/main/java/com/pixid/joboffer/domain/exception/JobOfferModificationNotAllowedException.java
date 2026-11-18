package com.pixid.joboffer.domain.exception;

import com.pixid.joboffer.domain.valueobject.JobOfferStatus;

public class JobOfferModificationNotAllowedException extends DomainException {
    public JobOfferModificationNotAllowedException(JobOfferStatus currentStatus) {
        super("Job offer cannot be modified while in status '" + currentStatus + "'. Allowed statuses: DRAFT, TO_FINALIZE.");
    }
}
