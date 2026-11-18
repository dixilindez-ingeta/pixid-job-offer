package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.exception.DomainValidationException;
import com.pixid.joboffer.domain.valueobject.CompanyConfig;
import com.pixid.joboffer.domain.valueobject.CompanyId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyConfigUseCasesTest {

    private final InMemoryCompanyConfigProvider provider = new InMemoryCompanyConfigProvider();
    private final GetCompanyConfig getCompanyConfig = new GetCompanyConfig(provider);
    private final UpdateCompanyConfig updateCompanyConfig = new UpdateCompanyConfig(provider);
    private final CompanyId companyId = CompanyId.generate();

    @Test
    @DisplayName("Configuration absente : valeurs par défaut")
    void defaultsWhenAbsent() {
        assertThat(getCompanyConfig.execute(companyId)).isEqualTo(CompanyConfig.defaults());
    }

    @Test
    @DisplayName("Mise à jour puis lecture, scopée par companyId")
    void updateThenGet() {
        CompanyConfig config = new CompanyConfig(true, false, true);

        assertThat(updateCompanyConfig.execute(companyId, config)).isEqualTo(config);
        assertThat(getCompanyConfig.execute(companyId)).isEqualTo(config);
        assertThat(getCompanyConfig.execute(CompanyId.generate())).isEqualTo(CompanyConfig.defaults());
    }

    @Test
    @DisplayName("Configuration nulle refusée")
    void nullConfigRejected() {
        assertThatThrownBy(() -> updateCompanyConfig.execute(companyId, null))
                .isInstanceOf(DomainValidationException.class);
    }
}
