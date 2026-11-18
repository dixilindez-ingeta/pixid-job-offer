package com.pixid.joboffer.domain.model;

import com.pixid.joboffer.domain.exception.DomainValidationException;
import com.pixid.joboffer.domain.exception.JobOfferModificationNotAllowedException;
import com.pixid.joboffer.domain.validator.Guard;
import com.pixid.joboffer.domain.valueobject.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

public class JobOffer {

    private static final int TITLE_MAX_LENGTH = 200;

    private final JobOfferId id;
    private final CompanyId companyId;
    private String title;
    private Location location;
    private Compensation compensation;
    private JobOfferStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private final Long version;

    private JobOffer(JobOfferId id, CompanyId companyId, String title, Location location, Compensation compensation,
                     JobOfferStatus status, Instant createdAt, Instant updatedAt, Instant publishedAt, Long version) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.location = location;
        this.compensation = compensation;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.version = version;
    }

    public static JobOffer createDraft(JobOfferId id, CompanyId companyId, String title, Location location,
                                       Compensation compensation, CompanyConfig config, Clock clock) {
        Guard.notNull(id, "Job offer ID is required");
        Guard.notNull(companyId, "Company ID is required");
        Guard.notNull(config, "Company config is required");
        validateTitle(title);

        if (!config.partialSaveEnabled()) {
            ensureComplete(title, location, compensation);
        }
        Instant now = clock.instant();
        return new JobOffer(id, companyId, title, location, compensation,
                JobOfferStatus.DRAFT, now, now, null, null);
    }

    public static JobOffer rehydrate(JobOfferId id, CompanyId companyId, String title, Location location,
                                     Compensation compensation, JobOfferStatus status, Instant createdAt,
                                     Instant updatedAt, Instant publishedAt, Long version) {
        return new JobOffer(id, companyId, title, location, compensation,
                status, createdAt, updatedAt, publishedAt, version);
    }

    // An offer must be complete as soon as it moves beyond TO_FINALIZE.
    private static boolean requiresCompleteness(TransitionAction action, JobOfferStatus target) {
        return (action == TransitionAction.SUBMIT || action == TransitionAction.FINALIZE)
                && target != JobOfferStatus.TO_FINALIZE;
    }

    private static void ensureComplete(String title, Location location, Compensation compensation) {
        List<String> missing = missingFields(title, location, compensation);
        if (!missing.isEmpty()) {
            throw new DomainValidationException("Job offer is incomplete, missing: " + String.join(", ", missing));
        }
    }

    private static List<String> missingFields(String title, Location location, Compensation compensation) {
        List<String> missing = new ArrayList<>();
        if (title == null || title.isBlank()) missing.add("title");
        if (location == null) missing.add("location");
        if (compensation == null) missing.add("compensation");
        return missing;
    }

    private static void validateTitle(String title) {
        Guard.maxLength(title, TITLE_MAX_LENGTH, "Title cannot exceed " + TITLE_MAX_LENGTH + " characters");
    }

    public void update(String title, Location location, Compensation compensation, Clock clock) {
        if (!isModifiable()) {
            throw new JobOfferModificationNotAllowedException(status);
        }
        if (title != null) {
            Guard.notBlank(title, "Title cannot be blank");
            validateTitle(title);
            this.title = title;
        }
        if (location != null) {
            this.location = location;
        }
        if (compensation != null) {
            this.compensation = compensation;
        }
        this.updatedAt = clock.instant();
    }

    public void apply(TransitionAction action, CompanyConfig config, Clock clock) {
        Guard.notNull(action, "Transition action is required");
        Guard.notNull(config, "Company config is required");
        JobOfferStatus target = JobOfferStateMachine.targetStatus(status, action, config);
        if (requiresCompleteness(action, target)) {
            ensureComplete(title, location, compensation);
        }
        Instant now = clock.instant();
        this.status = target;
        this.updatedAt = now;
        if (target == JobOfferStatus.PUBLISHED) {
            this.publishedAt = now;
        }
    }

    public Set<TransitionAction> availableActions(CompanyConfig config) {
        Set<TransitionAction> actions = EnumSet.noneOf(TransitionAction.class);
        for (TransitionAction action : JobOfferStateMachine.allowedActions(status)) {
            JobOfferStatus target = JobOfferStateMachine.targetStatus(status, action, config);
            if (!requiresCompleteness(action, target) || isComplete()) {
                actions.add(action);
            }
        }
        return Collections.unmodifiableSet(actions);
    }

    public boolean isComplete() {
        return missingFields(title, location, compensation).isEmpty();
    }

    public boolean isModifiable() {
        return status == JobOfferStatus.DRAFT || status == JobOfferStatus.TO_FINALIZE;
    }

    public JobOfferId getId() { return id; }
    public CompanyId getCompanyId() { return companyId; }
    public String getTitle() { return title; }
    public Location getLocation() { return location; }
    public Compensation getCompensation() { return compensation; }
    public JobOfferStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }

    public Long getVersion() {
        return version;
    }
}
