package com.pixid.joboffer.domain.valueobject;

import com.pixid.joboffer.domain.validator.Guard;

public record Location(LocationType type, Address address) {
    public Location {
        Guard.notNull(type, "Location type is required");

        if (type == LocationType.CUSTOM) {
            Guard.notNull(address, "Address is required when location type is CUSTOM");
        } else if (type == LocationType.COMPANY_ADDRESS) {
            Guard.isTrue(address == null, "Address must not be provided when location type is COMPANY_ADDRESS");
        }
    }
}
