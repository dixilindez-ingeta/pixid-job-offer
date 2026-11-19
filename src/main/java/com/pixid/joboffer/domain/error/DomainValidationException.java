package com.pixid.joboffer.domain.error;

public class DomainValidationException extends DomainException {
    private static final long serialVersionUID = 1L;

    public DomainValidationException(String message) {
        super(message);
    }
}