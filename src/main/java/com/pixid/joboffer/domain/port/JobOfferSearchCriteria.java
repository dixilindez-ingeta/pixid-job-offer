package com.pixid.joboffer.domain.port;

import com.pixid.joboffer.domain.valueobject.CompanyId;
import com.pixid.joboffer.domain.valueobject.JobOfferStatus;

/**
 * Optional filters: a {@code null} value means "no filter".
 */
public record JobOfferSearchCriteria(CompanyId companyId, JobOfferStatus status) {
}
