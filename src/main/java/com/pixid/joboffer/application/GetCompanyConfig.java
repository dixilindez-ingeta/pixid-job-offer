package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCompanyConfig {

    private final CompanyConfigProvider companyConfigProvider;

    public GetCompanyConfig(CompanyConfigProvider companyConfigProvider) {
        this.companyConfigProvider = companyConfigProvider;
    }

    @Transactional(readOnly = true)
    public CompanyConfig execute(CompanyId companyId) {
        return companyConfigProvider.findByCompanyId(companyId);
    }
}
