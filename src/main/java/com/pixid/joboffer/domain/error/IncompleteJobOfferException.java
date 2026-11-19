package com.pixid.joboffer.domain.error;

import java.util.List;

public class IncompleteJobOfferException extends DomainException {
    private static final long serialVersionUID = 1L;

    private final List<String> missingFields;

    public IncompleteJobOfferException(List<String> missingFields) {
        super("Job offer is incomplete, missing: " + String.join(", ", missingFields));
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
