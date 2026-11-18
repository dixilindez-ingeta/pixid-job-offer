package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.valueobject.CompanyId;
import com.pixid.joboffer.domain.valueobject.Compensation;
import com.pixid.joboffer.domain.valueobject.Location;

public record CreateJobOfferCommand(CompanyId companyId, String title, Location location, Compensation compensation) {
}
