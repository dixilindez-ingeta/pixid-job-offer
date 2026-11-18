package com.pixid.joboffer.domain.error;

public class ResourceNotFoundException extends DomainException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}