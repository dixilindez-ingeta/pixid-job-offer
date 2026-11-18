package com.pixid.joboffer.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "company_configs")
@Getter
@Setter
public class CompanyConfigJpaEntity {

    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    @Column(name = "partial_save_enabled", nullable = false)
    private boolean partialSaveEnabled;

    @Column(name = "manual_posting_required", nullable = false)
    private boolean manualPostingRequired;
}
