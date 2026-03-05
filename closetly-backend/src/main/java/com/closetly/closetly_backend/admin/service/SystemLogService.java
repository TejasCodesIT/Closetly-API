package com.closetly.closetly_backend.admin.service;

import com.closetly.closetly_backend.admin.dto.SystemLogDTO;
import com.closetly.closetly_backend.admin.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public Page<SystemLogDTO> getLogs(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return systemLogRepository.findAll(pageable)
                .map(log -> SystemLogDTO.builder()
                        .id(log.getId())
                        .type(log.getType().toString())
                        .message(log.getMessage())
                        .details(log.getDetails())
                        .createdAt(log.getCreatedAt())
                        .build())
                .map(dto -> dto); // Map to maintain chain
    }
}
