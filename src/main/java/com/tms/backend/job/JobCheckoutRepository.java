package com.tms.backend.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCheckoutRepository extends JpaRepository<JobCheckout, Long> {
    Optional<JobCheckout> findByJobId(Long jobId);

    List<JobCheckout> findByExpiresAtBefore(LocalDateTime time);
}
