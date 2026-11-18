package com.pixid.joboffer.infrastructure.persistence;

import com.pixid.joboffer.domain.model.JobOffer;
import com.pixid.joboffer.domain.port.JobOfferRepository;
import com.pixid.joboffer.domain.port.JobOfferSearchCriteria;
import com.pixid.joboffer.domain.port.PageRequest;
import com.pixid.joboffer.domain.port.PageResult;
import com.pixid.joboffer.domain.valueobject.JobOfferId;
import com.pixid.joboffer.infrastructure.persistence.entity.JobOfferJpaEntity;
import com.pixid.joboffer.infrastructure.persistence.mapper.JobOfferMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresJobOfferRepositoryAdapter implements JobOfferRepository {

    private final SpringDataJobOfferRepository jpaRepository;

    public PostgresJobOfferRepositoryAdapter(SpringDataJobOfferRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    private static Specification<JobOfferJpaEntity> toSpecification(JobOfferSearchCriteria criteria) {
        List<Specification<JobOfferJpaEntity>> specs = new ArrayList<>();
        if (criteria.companyId() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("companyId"), criteria.companyId().value()));
        }
        if (criteria.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), criteria.status().name()));
        }
        return Specification.allOf(specs);
    }

    private static org.springframework.data.domain.PageRequest toPageable(PageRequest pageRequest) {
        String property = switch (pageRequest.sortField()) {
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case PUBLISHED_AT -> "publishedAt";
        };
        Sort.Direction direction = pageRequest.direction() == PageRequest.SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        // Tie-breaker on id keeps pagination stable when timestamps are equal.
        Sort sort = Sort.by(direction, property).and(Sort.by(direction, "id"));
        return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), sort);
    }

    @Override
    @Transactional
    public JobOffer save(JobOffer jobOffer) {
        JobOfferJpaEntity entity = JobOfferMapper.toEntity(jobOffer);
        // Flush so that the returned aggregate carries the incremented version.
        JobOfferJpaEntity savedEntity = jpaRepository.saveAndFlush(entity);
        return JobOfferMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobOffer> findById(JobOfferId id) {
        return jpaRepository.findById(id.value())
                .map(JobOfferMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<JobOffer> search(JobOfferSearchCriteria criteria, PageRequest pageRequest) {
        Page<JobOfferJpaEntity> page = jpaRepository.findAll(toSpecification(criteria), toPageable(pageRequest));
        return new PageResult<>(
                page.getContent().stream().map(JobOfferMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
