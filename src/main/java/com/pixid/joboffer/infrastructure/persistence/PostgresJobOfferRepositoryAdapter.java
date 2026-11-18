package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.mapper.JobOfferMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresJobOfferRepositoryAdapter implements JobOfferRepository {

    private final SpringDataJobOfferRepository jpaRepository;

    public PostgresJobOfferRepositoryAdapter(SpringDataJobOfferRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public JobOffer save(JobOffer jobOffer) {
        JobOfferJpaEntity entity = JobOfferMapper.toEntity(jobOffer);
        JobOfferJpaEntity savedEntity = jpaRepository.save(entity);
        return JobOfferMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<JobOffer> findById(JobOfferId id) {
        return jpaRepository.findById(id.value())
                .map(JobOfferMapper::toDomain);
    }
}