package com.pixid.joboffer.presentation.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("un conflit de verrou optimiste est traduit en 409")
    void optimisticLockConflict() {
        ProblemDetail problem = handler.handleConcurrentModification(
                new ObjectOptimisticLockingFailureException("JobOfferJpaEntity", "id"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Concurrent modification");
    }
}
