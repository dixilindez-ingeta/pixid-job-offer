package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import com.pixid.joboffer.infrastructure.persistence.entity.CompanyConfigJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PostgresCompanyConfigAdapter implements CompanyConfigProvider {

    private final SpringDataCompanyConfigRepository jpaRepository;

    public PostgresCompanyConfigAdapter(SpringDataCompanyConfigRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    private static CompanyConfig toDomain(CompanyConfigJpaEntity entity) {
        return new CompanyConfig(
                entity.isPartialSaveEnabled(),
                entity.isApprovalRequired(),
                entity.isManualPostingRequired()
        );
    }

    @Override
    public CompanyConfig findByCompanyId(CompanyId companyId) {
        return jpaRepository.findById(companyId.value())
                .map(PostgresCompanyConfigAdapter::toDomain)
                .orElseGet(CompanyConfig::defaults);
    }

    @Override
    public CompanyConfig save(CompanyId companyId, CompanyConfig config) {
        CompanyConfigJpaEntity entity = jpaRepository.findById(companyId.value())
                .orElseGet(() -> {
                    CompanyConfigJpaEntity created = new CompanyConfigJpaEntity();
                    created.setCompanyId(companyId.value());
                    return created;
                });
        entity.setPartialSaveEnabled(config.requireFinalization());
        entity.setApprovalRequired(config.requireApproval());
        entity.setManualPostingRequired(config.requirePostReview());
        return toDomain(jpaRepository.save(entity));
    }
}
