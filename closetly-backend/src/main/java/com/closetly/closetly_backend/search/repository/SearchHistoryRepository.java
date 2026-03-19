package com.closetly.closetly_backend.search.repository;

import com.closetly.closetly_backend.search.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findTop20ByUserEmailOrderByCreatedAtDesc(String email);
}

