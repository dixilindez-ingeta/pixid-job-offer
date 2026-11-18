package com.pixid.joboffer.infrastructure.persistence.mapper;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.valueobject.*;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferBonusJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferSalaryJpaEntity;

import java.util.List;
import java.util.stream.Collectors;

public final class JobOfferMapper {

    private JobOfferMapper() {
    }

    public static JobOfferJpaEntity toEntity(JobOffer domain) {
        JobOfferJpaEntity entity = new JobOfferJpaEntity();
        entity.setId(domain.getId().value());
        entity.setCompanyId(domain.getCompanyId().value());
        entity.setTitle(domain.getTitle());

        // Mapping de la Location
        Location loc = domain.getLocation();
        entity.setLocationType(loc.type().name());
        if (loc.address() != null) {
            List<String> lines = loc.address().lines();
            if (lines != null) {
                if (!lines.isEmpty()) entity.setAddressLine1(lines.get(0));
                if (lines.size() > 1) entity.setAddressLine2(lines.get(1));
                if (lines.size() > 2) entity.setAddressLine3(lines.get(2));
            }
            entity.setPostalCode(loc.address().postalCode());
            entity.setCity(loc.address().city());
            entity.setRegion(loc.address().region());
            entity.setCountryCode(loc.address().countryCode());
        }

        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPublishedAt(domain.getPublishedAt());

        // Mapping des entrées de salaires (SalaryEntries)
        List<JobOfferSalaryJpaEntity> salaryEntities = domain.getCompensation().salaryEntries().stream().map(s -> {
            JobOfferSalaryJpaEntity salaryEntity = new JobOfferSalaryJpaEntity();
            salaryEntity.setJobOffer(entity);
            salaryEntity.setType(s.type().name());
            salaryEntity.setAmount(s.amount());
            salaryEntity.setCurrency(s.currency());
            return salaryEntity;
        }).collect(Collectors.toList());
        entity.setSalaryEntries(salaryEntities);

        // Mapping des entrées de bonus (BonusEntries)
        List<JobOfferBonusJpaEntity> bonusEntities = domain.getCompensation().bonusEntries().stream().map(b -> {
            JobOfferBonusJpaEntity bonusEntity = new JobOfferBonusJpaEntity();
            bonusEntity.setJobOffer(entity);
            bonusEntity.setType(b.type().name());
            bonusEntity.setAmount(b.amount());
            bonusEntity.setCurrency(b.currency());
            return bonusEntity;
        }).collect(Collectors.toList());
        entity.setBonusEntries(bonusEntities);

        return entity;
    }

    public static JobOffer toDomain(JobOfferJpaEntity entity) {
        JobOfferId id = new JobOfferId(entity.getId());
        CompanyId companyId = new CompanyId(entity.getCompanyId());

        // Reconstruction de Location
        Address address = null;
        if ("CUSTOM".equals(entity.getLocationType())) {
            List<String> lines = List.of(
                    entity.getAddressLine1() != null ? entity.getAddressLine1() : "",
                    entity.getAddressLine2() != null ? entity.getAddressLine2() : "",
                    entity.getAddressLine3() != null ? entity.getAddressLine3() : ""
            ).stream().filter(s -> !s.isBlank()).toList();

            address = new Address(lines, entity.getPostalCode(), entity.getCity(), entity.getRegion(), entity.getCountryCode());
        }
        Location location = new Location(LocationType.valueOf(entity.getLocationType()), address);

        // Reconstruction de Compensation (Salaires & Bonus)
        List<SalaryEntry> salaryEntries = entity.getSalaryEntries().stream()
                .map(s -> new SalaryEntry(
                        SalaryType.valueOf(s.getType()),
                        s.getAmount(),
                        s.getCurrency()
                )).toList();

        List<BonusEntry> bonusEntries = entity.getBonusEntries().stream()
                .map(b -> new BonusEntry(
                        BonusType.valueOf(b.getType()),
                        b.getAmount(),
                        b.getCurrency()
                )).toList();

        Compensation compensation = new Compensation(salaryEntries, bonusEntries);

        return new JobOffer(id, companyId, entity.getTitle(), location, compensation);
    }
}