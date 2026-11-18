package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.exception.DomainValidationException;
import com.pixid.joboffer.domain.validator.Guard;
import com.pixid.joboffer.domain.valueobject.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public class JobOffer {
    private final JobOfferId id;
    private final CompanyId companyId;
    private String title;
    private Location location;
    private Compensation compensation;
    private JobOfferStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    public JobOffer(JobOfferId id, CompanyId companyId, String title, Location location, Compensation compensation) {
        Guard.notNull(id, "Job offer ID is required");
        Guard.notNull(companyId, "Company ID is required");
        validateFields(title, location, compensation, false);

        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.location = location;
        this.compensation = compensation;
        this.status = JobOfferStatus.DRAFT;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.publishedAt = null;
    }

    public void update(String title, Location location, Compensation compensation) {
        if (this.status != JobOfferStatus.DRAFT && this.status != JobOfferStatus.TO_FINALIZE) {
            throw new DomainValidationException(
                    "Job offer can only be updated when in DRAFT or TO_FINALIZE status. Current: " + this.status
            );
        }
        validateFields(title, location, compensation, true);

        if (title != null) {
            this.title = title;
        }

        if (location != null) {
            this.location = location;
        }

        if (compensation != null) {
            this.compensation = compensation;
        }

        this.updatedAt = Instant.now();
    }

    public void advance(CompanyConfig config) {
        JobOfferStatus nextStatus = JobOfferStateMachine.getNextStatus(this.status, config);
        this.status = nextStatus;
        this.updatedAt = Instant.now();

        if (this.status == JobOfferStatus.PUBLISHED) {
            this.publishedAt = Instant.now();
        }
    }

    public void close() {
        if (this.status == JobOfferStatus.CLOSED) {
            throw new DomainValidationException("Job offer is already closed");
        }
        this.status = JobOfferStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public Set<JobOfferStatus> getAvailableTransitions(CompanyConfig config) {
        if (this.status == JobOfferStatus.CLOSED || this.status == JobOfferStatus.PUBLISHED) {
            return this.status == JobOfferStatus.PUBLISHED ? Set.of(JobOfferStatus.CLOSED) : Set.of();
        }
        try {
            JobOfferStatus next = JobOfferStateMachine.getNextStatus(this.status, config);
            return EnumSet.of(next, JobOfferStatus.CLOSED);
        } catch (Exception e) {
            return Set.of(JobOfferStatus.CLOSED);
        }
    }

    private static void validateFields(String title, Location location, Compensation compensation, boolean allowPartial) {
        if (title != null) {
            Guard.maxLength(title, 200, "Title cannot exceed 200 characters");
        }

        if (!allowPartial) {
            Guard.notBlank(title, "Title is required");
            Guard.notNull(location, "Location is required");
            Guard.notNull(compensation, "Compensation is required");
        }
    }

    // Getters
    public JobOfferId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public Location getLocation() { return location; }
    public Compensation getCompensation() { return compensation; }
    public JobOfferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
}