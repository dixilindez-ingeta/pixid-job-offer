package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.error.ResourceNotFoundException;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetJobOffer {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyConfigProvider companyConfigProvider;

    public GetJobOffer(JobOfferRepository jobOfferRepository, CompanyConfigProvider companyConfigProvider) {
        this.jobOfferRepository = jobOfferRepository;
        this.companyConfigProvider = companyConfigProvider;
    }

    @Transactional(readOnly = true)
    public JobOfferDetails execute(JobOfferId id) {
        JobOffer jobOffer = jobOfferRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.jobOffer(id));
        return JobOfferDetails.of(jobOffer, companyConfigProvider.findByCompanyId(jobOffer.getCompanyId()));
    }
}
