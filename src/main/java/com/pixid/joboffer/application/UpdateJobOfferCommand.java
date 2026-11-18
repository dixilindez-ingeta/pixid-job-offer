package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.valueobject.Compensation;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import com.pixid.joboffer.domain.valueobject.Location;

public record UpdateJobOfferCommand(JobOfferId id, String title, Location location, Compensation compensation) {
}
