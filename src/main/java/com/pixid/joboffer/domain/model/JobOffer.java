package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.error.JobOfferModificationNotAllowedException;
import com.pixid.joboffer.domain.service.JobOfferStateMachine;
import com.pixid.joboffer.domain.validator.Guard;
import com.pixid.joboffer.domain.valueobject.*;

import java.time.Instant;
import java.util.Set;

public class JobOffer {

    private final JobOfferId id;
    private final CompanyId companyId;
    private String title;
    private JobOfferStatus status;
    private Location location;
    private Compensation compensation;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    // Constructeur pour reconstitution depuis la persistance
    public JobOffer(
            JobOfferId id,
            CompanyId companyId,
            String title,
            JobOfferStatus status,
            Location location,
            Compensation compensation,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.status = status;
        this.location = location;
        this.compensation = compensation;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
    }

    // Factory method pour la création d'un brouillon
    public static JobOffer createDraft(
            CompanyId companyId,
            String title,
            Location location,
            Compensation compensation,
            CompanyConfig config,
            Instant now
    ) {
        Guard.notNull(companyId, "CompanyId is required");
        validateFields(title, location, compensation, config.partialSaveEnabled());

        return new JobOffer(
                JobOfferId.generate(),
                companyId,
                title,
                JobOfferStatus.DRAFT,
                location,
                compensation,
                now,
                now,
                null
        );
    }

    public void update(
            String title,
            Location location,
            Compensation compensation,
            CompanyConfig config,
            Instant now
    ) {
        if (this.status != JobOfferStatus.DRAFT && this.status != JobOfferStatus.TO_FINALIZE) {
            throw new JobOfferModificationNotAllowedException(this.status);
        }
        boolean allowPartial = config.partialSaveEnabled() && this.status == JobOfferStatus.DRAFT;
        validateFields(title, location, compensation, allowPartial);

        this.title = title;
        this.location = location;
        this.compensation = compensation;
        this.updatedAt = now;
    }

    public void transition(TransitionAction action, CompanyConfig config, Instant now) {
        // Avant de finaliser ou de soumettre sans sauvegarde partielle, l'offre doit être complète
        if (action == TransitionAction.FINALIZE ||
                (action == TransitionAction.SUBMIT && !config.partialSaveEnabled())) {
            validateCompleteness();
        }

        JobOfferStatus nextStatus = JobOfferStateMachine.computeNextStatus(this.status, action, config);
        this.status = nextStatus;
        this.updatedAt = now;

        if (nextStatus == JobOfferStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public Set<TransitionAction> getAvailableTransitions(CompanyConfig config) {
        return JobOfferStateMachine.getAvailableTransitions(this.status, config);
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

    private void validateCompleteness() {
        validateFields(this.title, this.location, this.compensation, false);
    }

    // Getters
    public JobOfferId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public JobOfferStatus getStatus() { return status; }
    public Location getLocation() { return location; }
    public Compensation getCompensation() { return compensation; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
}
