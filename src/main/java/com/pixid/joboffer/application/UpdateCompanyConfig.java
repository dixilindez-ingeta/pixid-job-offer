package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.validator.Guard;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCompanyConfig {

    private final CompanyConfigProvider companyConfigProvider;

    public UpdateCompanyConfig(CompanyConfigProvider companyConfigProvider) {
        this.companyConfigProvider = companyConfigProvider;
    }

    @Transactional
    public CompanyConfig execute(CompanyId companyId, CompanyConfig config) {
        Guard.notNull(config, "Company config is required");
        return companyConfigProvider.save(companyId, config);
    }
}
