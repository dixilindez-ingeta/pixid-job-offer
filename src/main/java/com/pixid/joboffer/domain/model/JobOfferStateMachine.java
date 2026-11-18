package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.exception.DomainValidationException;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;

public final class JobOfferStateMachine {

    private JobOfferStateMachine() {}

    public static JobOfferStatus getNextStatus(JobOfferStatus currentStatus, CompanyConfig config) {
        return switch (currentStatus) {
            case DRAFT -> config.requireFinalization() ? JobOfferStatus.TO_FINALIZE : getNextAfterFinalize(config);
            case TO_FINALIZE -> getNextAfterFinalize(config);
            case TO_APPROVE -> getNextAfterApprove(config);
            case TO_POST -> JobOfferStatus.PUBLISHED;
            case PUBLISHED, CLOSED -> throw new DomainValidationException(
                    "Cannot transition forward from terminal or published state: " + currentStatus
            );
        };
    }

    private static JobOfferStatus getNextAfterFinalize(CompanyConfig config) {
        if (config.requireApproval()) {
            return JobOfferStatus.TO_APPROVE;
        }
        return getNextAfterApprove(config);
    }

    private static JobOfferStatus getNextAfterApprove(CompanyConfig config) {
        if (config.requirePostReview()) {
            return JobOfferStatus.TO_POST;
        }
        return JobOfferStatus.PUBLISHED;
    }
}