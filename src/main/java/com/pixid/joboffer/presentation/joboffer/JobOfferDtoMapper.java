package com.pixid.joboffer.presentation.joboffer;

import com.pixid.joboffer.application.JobOfferDetails;
import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.PageResult;
import com.pixid.joboffer.domain.valueobject.*;
import com.pixid.joboffer.presentation.joboffer.dto.*;

import java.util.List;

final class JobOfferDtoMapper {

    private JobOfferDtoMapper() {
    }

    static Location toLocation(LocationDto dto) {
        if (dto == null) {
            return null;
        }
        AddressDto a = dto.address();
        Address address = a == null ? null
                : new Address(a.lines(), a.postalCode(), a.city(), a.region(), a.countryCode());
        return new Location(dto.type(), address);
    }

    static Compensation toCompensation(CompensationDto dto) {
        if (dto == null) {
            return null;
        }
        List<SalaryEntry> salaries = dto.salaryEntries() == null ? List.of() : dto.salaryEntries().stream()
                .map(s -> new SalaryEntry(s.type(), s.amount(), s.currency()))
                .toList();
        List<BonusEntry> bonuses = dto.bonusEntries() == null ? List.of() : dto.bonusEntries().stream()
                .map(b -> new BonusEntry(b.type(), b.amount(), b.currency()))
                .toList();
        return new Compensation(salaries, bonuses);
    }

    static JobOfferResponse toResponse(JobOfferDetails details) {
        List<TransitionAction> transitions = details.availableActions().stream().sorted().toList();
        return toResponse(details.jobOffer(), transitions);
    }

    static PageResponse<JobOfferResponse> toResponse(PageResult<JobOffer> page) {
        return new PageResponse<>(
                page.content().stream().map(offer -> toResponse(offer, null)).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    private static JobOfferResponse toResponse(JobOffer offer, List<TransitionAction> transitions) {
        return new JobOfferResponse(
                offer.getId().value(),
                offer.getCompanyId().value(),
                offer.getTitle(),
                offer.getStatus(),
                toDto(offer.getLocation()),
                toDto(offer.getCompensation()),
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                offer.getPublishedAt(),
                transitions);
    }

    private static LocationDto toDto(Location location) {
        if (location == null) {
            return null;
        }
        Address a = location.address();
        AddressDto address = a == null ? null
                : new AddressDto(a.lines(), a.postalCode(), a.city(), a.region(), a.countryCode());
        return new LocationDto(location.type(), address);
    }

    private static CompensationDto toDto(Compensation compensation) {
        if (compensation == null) {
            return null;
        }
        return new CompensationDto(
                compensation.salaryEntries().stream()
                        .map(s -> new SalaryEntryDto(s.type(), s.amount(), s.currency())).toList(),
                compensation.bonusEntries().stream()
                        .map(b -> new BonusEntryDto(b.type(), b.amount(), b.currency())).toList());
    }
}
