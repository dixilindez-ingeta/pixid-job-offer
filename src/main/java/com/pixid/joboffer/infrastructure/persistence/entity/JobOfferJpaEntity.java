package com.pixid.joboffer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
public class JobOfferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(length = 200)
    private String title;

    // Location fields
    @Column(name = "location_type", length = 50)
    private String locationType;

    @Column(name = "address_line1", length = 100)
    private String addressLine1;

    @Column(name = "address_line2", length = 100)
    private String addressLine2;

    @Column(name = "address_line3", length = 100)
    private String addressLine3;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String region;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    // Relations pour les listes de salaires et de bonus
    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobOfferSalaryJpaEntity> salaryEntries = new ArrayList<>();

    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobOfferBonusJpaEntity> bonusEntries = new ArrayList<>();
}