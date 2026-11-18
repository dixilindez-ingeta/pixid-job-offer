package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.port.JobOfferSearchCriteria;
import com.pixid.joboffer.domain.port.PageRequest;
import com.pixid.joboffer.domain.port.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchJobOffers {

    private final JobOfferRepository jobOfferRepository;

    public SearchJobOffers(JobOfferRepository jobOfferRepository) {
        this.jobOfferRepository = jobOfferRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<JobOffer> execute(JobOfferSearchCriteria criteria, PageRequest pageRequest) {
        return jobOfferRepository.search(criteria, pageRequest);
    }
}
