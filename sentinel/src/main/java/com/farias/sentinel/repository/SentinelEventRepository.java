package com.farias.sentinel.repository;

import com.farias.sentinel.model.SentinelEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentinelEventRepository extends JpaRepository<SentinelEvent, Long> {
}
