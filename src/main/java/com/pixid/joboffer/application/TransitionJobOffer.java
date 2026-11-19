package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.error.ResourceNotFoundException;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import com.pixid.joboffer.domain.valueobject.TransitionAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class TransitionJobOffer {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyConfigProvider companyConfigProvider;
    private final Clock clock;

    public TransitionJobOffer(JobOfferRepository jobOfferRepository, CompanyConfigProvider companyConfigProvider, Clock clock) {
        this.jobOfferRepository = jobOfferRepository;
        this.companyConfigProvider = companyConfigProvider;
        this.clock = clock;
    }

    @Transactional
    public JobOfferDetails execute(JobOfferId id, TransitionAction action) {
        JobOffer jobOffer = jobOfferRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.jobOffer(id));
        CompanyConfig config = companyConfigProvider.findByCompanyId(jobOffer.getCompanyId());
        jobOffer.apply(action, config, clock);
        return JobOfferDetails.of(jobOfferRepository.save(jobOffer), config);
    }
}
