package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SpringDataJobOfferRepository
        extends JpaRepository<JobOfferJpaEntity, UUID>, JpaSpecificationExecutor<JobOfferJpaEntity> {
}
