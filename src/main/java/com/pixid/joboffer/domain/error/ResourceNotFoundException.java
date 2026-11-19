package com.pixid.joboffer.domain.error;

import com.pixid.joboffer.domain.valueobject.JobOfferId;

public class ResourceNotFoundException extends DomainException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException jobOffer(JobOfferId id) {
        return new ResourceNotFoundException("Job offer '" + id.value() + "' not found");
    }
}
