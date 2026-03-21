package com.closetly.closetly_backend.admin.service;

import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.closetly.closetly_backend.admin.dto.SystemLogDTO;
import com.closetly.closetly_backend.admin.mapper.AdminMapper;
import com.closetly.closetly_backend.admin.repository.SystemLogRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    public Page<SystemLogDTO> getLogs(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return systemLogRepository.findAll(pageable)
                .map(AdminMapper::toSystemLogDTO);
    }
}
