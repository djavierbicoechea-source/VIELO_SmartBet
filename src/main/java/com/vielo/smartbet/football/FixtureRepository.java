package com.vielo.smartbet.football;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FixtureRepository extends JpaRepository<FixtureEntity, Long> {
    List<FixtureEntity> findByKickoffBetweenOrderByKickoffAsc(LocalDateTime start, LocalDateTime end);
}
