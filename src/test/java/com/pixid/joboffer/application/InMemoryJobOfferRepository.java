package com.pixid.joboffer.application;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.port.JobOfferSearchCriteria;
import com.pixid.joboffer.domain.port.PageRequest;
import com.pixid.joboffer.domain.port.PageResult;
import com.pixid.joboffer.domain.valueobject.JobOfferId;

import java.util.*;

class InMemoryJobOfferRepository implements JobOfferRepository {

    private final Map<JobOfferId, JobOffer> store = new HashMap<>();
    int saveCount;

    @Override
    public JobOffer save(JobOffer jobOffer) {
        saveCount++;
        long nextVersion = jobOffer.getVersion() == null ? 0 : jobOffer.getVersion() + 1;
        JobOffer saved = JobOffer.rehydrate(jobOffer.getId(), jobOffer.getCompanyId(), jobOffer.getTitle(),
                jobOffer.getLocation(), jobOffer.getCompensation(), jobOffer.getStatus(), jobOffer.getCreatedAt(),
                jobOffer.getUpdatedAt(), jobOffer.getPublishedAt(), nextVersion);
        store.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public Optional<JobOffer> findById(JobOfferId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public PageResult<JobOffer> search(JobOfferSearchCriteria criteria, PageRequest pageRequest) {
        List<JobOffer> matching = store.values().stream()
                .filter(o -> criteria.companyId() == null || o.getCompanyId().equals(criteria.companyId()))
                .filter(o -> criteria.status() == null || o.getStatus() == criteria.status())
                .sorted(Comparator.comparing(JobOffer::getCreatedAt))
                .toList();
        int from = Math.min(pageRequest.page() * pageRequest.size(), matching.size());
        int to = Math.min(from + pageRequest.size(), matching.size());
        int totalPages = (int) Math.ceil((double) matching.size() / pageRequest.size());
        return new PageResult<>(matching.subList(from, to), pageRequest.page(), pageRequest.size(),
                matching.size(), totalPages);
    }
}
