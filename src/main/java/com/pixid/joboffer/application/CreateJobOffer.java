package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CreateJobOffer {

    private final JobOfferRepository jobOfferRepository;
    private final CompanyConfigProvider companyConfigProvider;
    private final Clock clock;

    public CreateJobOffer(JobOfferRepository jobOfferRepository, CompanyConfigProvider companyConfigProvider, Clock clock) {
        this.jobOfferRepository = jobOfferRepository;
        this.companyConfigProvider = companyConfigProvider;
        this.clock = clock;
    }

    @Transactional
    public JobOfferDetails execute(CreateJobOfferCommand command) {
        CompanyConfig config = companyConfigProvider.findByCompanyId(command.companyId());
        JobOffer draft = JobOffer.createDraft(JobOfferId.generate(), command.companyId(), command.title(),
                command.location(), command.compensation(), config, clock);
        return JobOfferDetails.of(jobOfferRepository.save(draft), config);
    }
}
