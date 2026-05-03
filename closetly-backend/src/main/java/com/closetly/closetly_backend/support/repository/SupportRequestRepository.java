package com.closetly.closetly_backend.support.repository;

import com.closetly.closetly_backend.support.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
}
