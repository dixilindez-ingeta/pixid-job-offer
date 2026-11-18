package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

import java.math.BigDecimal;

public record SalaryEntry(SalaryType type, BigDecimal amount, String currency) {
    public SalaryEntry {
        Guard.notNull(type, "Salary type is required");
        Guard.notNull(amount, "Salary amount is required");
        Guard.greaterThanOrEqual(amount, BigDecimal.ZERO, "Salary amount must be >= 0");
        Guard.validIsoCurrency(currency, "Invalid ISO 4217 currency code: " + currency);

        currency = currency.toUpperCase();
    }
}