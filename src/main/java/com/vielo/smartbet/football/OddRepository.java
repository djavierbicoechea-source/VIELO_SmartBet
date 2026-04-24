package com.vielo.smartbet.football;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OddRepository extends JpaRepository<OddEntity, Long> {
    List<OddEntity> findByFixtureId(Long fixtureId);
    void deleteByFixtureId(Long fixtureId);
}
