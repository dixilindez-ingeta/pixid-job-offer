package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.math.BigDecimal;

public record BonusEntry(BonusType type, BigDecimal amount, String currency) {
    public BonusEntry {
        Guard.notNull(type, "Bonus type is required");
        Guard.notNull(amount, "Bonus amount is required");
        Guard.greaterThanOrEqual(amount, BigDecimal.ZERO, "Bonus amount must be >= 0");
        Guard.validIsoCurrency(currency, "Invalid ISO 4217 currency code: " + currency);

        currency = currency.toUpperCase();
    }
}
