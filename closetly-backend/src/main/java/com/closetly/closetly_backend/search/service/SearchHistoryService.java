package com.closetly.closetly_backend.search.service;

import com.closetly.closetly_backend.search.dto.SearchHistoryDTO;

import java.util.List;

public interface SearchHistoryService {
    void recordSearch(String email, String query);

    List<SearchHistoryDTO> getMyHistory(String email);
}

