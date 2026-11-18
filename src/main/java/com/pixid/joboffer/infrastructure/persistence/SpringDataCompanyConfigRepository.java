package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.infrastructure.persistence.entity.CompanyConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCompanyConfigRepository extends JpaRepository<CompanyConfigJpaEntity, UUID> {
}