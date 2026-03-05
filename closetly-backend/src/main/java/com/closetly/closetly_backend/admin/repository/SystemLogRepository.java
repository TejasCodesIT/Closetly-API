package com.closetly.closetly_backend.admin.repository;

import com.closetly.closetly_backend.admin.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    @Query("""
                SELECT sl FROM SystemLog sl
                WHERE (:type IS NULL OR sl.type = :type)
                ORDER BY sl.createdAt DESC
            """)
    Page<SystemLog> findLogs(
            @Param("type") SystemLog.LogType type,
            Pageable pageable);
}
