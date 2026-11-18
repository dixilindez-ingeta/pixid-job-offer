package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.TransitionAction;

import java.util.Set;

public record JobOfferDetails(JobOffer jobOffer, Set<TransitionAction> availableActions) {

    static JobOfferDetails of(JobOffer jobOffer, CompanyConfig config) {
        return new JobOfferDetails(jobOffer, jobOffer.availableActions(config));
    }
}
