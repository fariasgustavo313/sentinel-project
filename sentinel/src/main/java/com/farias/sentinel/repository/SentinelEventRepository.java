package com.farias.sentinel.repository;

import com.farias.sentinel.model.ContainerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SentinelEventRepository extends JpaRepository<ContainerEvent, Long> {
}
