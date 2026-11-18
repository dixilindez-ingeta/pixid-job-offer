package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.port.CompanyConfigProvider;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;

import java.util.HashMap;
import java.util.Map;

class InMemoryCompanyConfigProvider implements CompanyConfigProvider {

    private final Map<CompanyId, CompanyConfig> store = new HashMap<>();

    @Override
    public CompanyConfig findByCompanyId(CompanyId companyId) {
        return store.getOrDefault(companyId, CompanyConfig.defaults());
    }

    @Override
    public CompanyConfig save(CompanyId companyId, CompanyConfig config) {
        store.put(companyId, config);
        return config;
    }
}
