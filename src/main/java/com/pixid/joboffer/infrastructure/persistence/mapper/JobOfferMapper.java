package com.pixid.joboffer.infrastructure.persistence.mapper;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.valueobject.*;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferBonusJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferSalaryJpaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class JobOfferMapper {

    private JobOfferMapper() {
    }

    public static JobOfferJpaEntity toEntity(JobOffer domain) {
        JobOfferJpaEntity entity = new JobOfferJpaEntity();
        entity.setId(domain.getId().value());
        entity.setCompanyId(domain.getCompanyId().value());
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus().name());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPublishedAt(domain.getPublishedAt());
        entity.setVersion(domain.getVersion());

        Location location = domain.getLocation();
        if (location != null) {
            entity.setLocationType(location.type().name());
            Address address = location.address();
            if (address != null) {
                List<String> lines = address.lines();
                entity.setAddressLine1(lines.size() > 0 ? lines.get(0) : null);
                entity.setAddressLine2(lines.size() > 1 ? lines.get(1) : null);
                entity.setAddressLine3(lines.size() > 2 ? lines.get(2) : null);
                entity.setPostalCode(address.postalCode());
                entity.setCity(address.city());
                entity.setRegion(address.region());
                entity.setCountryCode(address.countryCode());
            }
        }

        Compensation compensation = domain.getCompensation();
        if (compensation != null) {
            entity.setSalaryEntries(compensation.salaryEntries().stream().map(s -> {
                JobOfferSalaryJpaEntity salary = new JobOfferSalaryJpaEntity();
                salary.setJobOffer(entity);
                salary.setType(s.type().name());
                salary.setAmount(s.amount());
                salary.setCurrency(s.currency());
                return salary;
            }).collect(ArrayList::new, List::add, List::addAll));
            entity.setBonusEntries(compensation.bonusEntries().stream().map(b -> {
                JobOfferBonusJpaEntity bonus = new JobOfferBonusJpaEntity();
                bonus.setJobOffer(entity);
                bonus.setType(b.type().name());
                bonus.setAmount(b.amount());
                bonus.setCurrency(b.currency());
                return bonus;
            }).collect(ArrayList::new, List::add, List::addAll));
        }

        return entity;
    }

    public static JobOffer toDomain(JobOfferJpaEntity entity) {
        return JobOffer.rehydrate(
                new JobOfferId(entity.getId()),
                new CompanyId(entity.getCompanyId()),
                entity.getTitle(),
                toLocation(entity),
                toCompensation(entity),
                JobOfferStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt(),
                entity.getVersion()
        );
    }

    private static Location toLocation(JobOfferJpaEntity entity) {
        if (entity.getLocationType() == null) {
            return null;
        }
        LocationType type = LocationType.valueOf(entity.getLocationType());
        if (type != LocationType.CUSTOM) {
            return new Location(type, null);
        }
        List<String> lines = Stream.of(entity.getAddressLine1(), entity.getAddressLine2(), entity.getAddressLine3())
                .filter(line -> line != null && !line.isBlank())
                .toList();
        Address address = new Address(lines, entity.getPostalCode(), entity.getCity(),
                entity.getRegion(), entity.getCountryCode());
        return new Location(type, address);
    }

    private static Compensation toCompensation(JobOfferJpaEntity entity) {
        List<SalaryEntry> salaries = entity.getSalaryEntries().stream()
                .map(s -> new SalaryEntry(SalaryType.valueOf(s.getType()), s.getAmount(), s.getCurrency()))
                .toList();
        List<BonusEntry> bonuses = entity.getBonusEntries().stream()
                .map(b -> new BonusEntry(BonusType.valueOf(b.getType()), b.getAmount(), b.getCurrency()))
                .toList();
        if (salaries.isEmpty() && bonuses.isEmpty()) {
            return null;
        }
        return new Compensation(salaries, bonuses);
    }
}
