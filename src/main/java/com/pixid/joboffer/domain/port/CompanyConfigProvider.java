package com.pixid.joboffer.domain.port;

import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;

public interface CompanyConfigProvider {
    CompanyConfig findByCompanyId(CompanyId companyId);

    CompanyConfig save(CompanyId companyId, CompanyConfig config);
}
