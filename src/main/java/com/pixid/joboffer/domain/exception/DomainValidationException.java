package com.pixid.joboffer.domain.exception;

public class DomainValidationException extends DomainException {
    private static final long serialVersionUID = 1L;

    public DomainValidationException(String message) {
        super(message);
    }
}