package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.exception.ResourceNotFoundException;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class UpdateJobOffer {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyConfigProvider companyConfigProvider;
    private final Clock clock;

    public UpdateJobOffer(JobOfferRepository jobOfferRepository, CompanyConfigProvider companyConfigProvider, Clock clock) {
        this.jobOfferRepository = jobOfferRepository;
        this.companyConfigProvider = companyConfigProvider;
        this.clock = clock;
    }

    @Transactional
    public JobOfferDetails execute(UpdateJobOfferCommand command) {
        JobOffer jobOffer = jobOfferRepository.findById(command.id())
                .orElseThrow(() -> ResourceNotFoundException.jobOffer(command.id()));
        jobOffer.update(command.title(), command.location(), command.compensation(), clock);
        JobOffer saved = jobOfferRepository.save(jobOffer);
        return JobOfferDetails.of(saved, companyConfigProvider.findByCompanyId(saved.getCompanyId()));
    }
}
