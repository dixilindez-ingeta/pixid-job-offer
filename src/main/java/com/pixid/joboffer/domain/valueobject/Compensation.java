package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.util.List;

public record Compensation(
        List<SalaryEntry> salaryEntries,
        List<BonusEntry> bonusEntries
) {
    public Compensation {
        salaryEntries = (salaryEntries == null) ? List.of() : List.copyOf(salaryEntries);
        bonusEntries = (bonusEntries == null) ? List.of() : List.copyOf(bonusEntries);

        Guard.isTrue(
                !salaryEntries.isEmpty() || !bonusEntries.isEmpty(),
                "Compensation must contain at least one salary or bonus entry"
        );
    }
}
