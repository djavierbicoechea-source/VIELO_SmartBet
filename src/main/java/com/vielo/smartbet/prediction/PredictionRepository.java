// ===============================
// PredictionRepository.java
// ===============================
package com.vielo.smartbet.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByForDateOrderByScoreDesc(LocalDate date);

    List<Prediction> findByForDateInOrderByForDateAscScoreDesc(List<LocalDate> dates);

    List<Prediction> findAllByOrderByForDateAscScoreDesc();

    boolean existsByForDate(LocalDate date);

    @Modifying
    @Transactional
    @Query("delete from Prediction p where p.forDate = :date")
    void deleteByForDate(@Param("date") LocalDate date);
}