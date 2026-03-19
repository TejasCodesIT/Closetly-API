package com.closetly.closetly_backend.search.service;

import com.closetly.closetly_backend.search.dto.SearchHistoryDTO;
import com.closetly.closetly_backend.search.entity.SearchHistory;
import com.closetly.closetly_backend.search.repository.SearchHistoryRepository;
import com.closetly.closetly_backend.user.entity.User;
import com.closetly.closetly_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordSearch(String email, String query) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SearchHistory history = SearchHistory.builder()
                .user(user)
                .query(query.trim())
                .build();

        searchHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryDTO> getMyHistory(String email) {
        return searchHistoryRepository.findTop20ByUserEmailOrderByCreatedAtDesc(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SearchHistoryDTO toDto(SearchHistory h) {
        SearchHistoryDTO dto = new SearchHistoryDTO();
        dto.setId(h.getId());
        dto.setQuery(h.getQuery());
        dto.setTimestamp(h.getCreatedAt());
        return dto;
    }
}

