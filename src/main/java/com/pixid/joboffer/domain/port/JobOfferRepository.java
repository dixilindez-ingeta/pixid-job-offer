package com.pixid.joboffer.domain.port;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.valueobject.JobOfferId;

import java.util.Optional;

public interface JobOfferRepository {
    JobOffer save(JobOffer jobOffer);

    Optional<JobOffer> findById(JobOfferId id);

    PageResult<JobOffer> search(JobOfferSearchCriteria criteria, PageRequest pageRequest);
}
