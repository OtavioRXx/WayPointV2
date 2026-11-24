package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.HorarioParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface HorarioParadaRepository extends JpaRepository<HorarioParada, Long> {

    List<HorarioParada> findByParadaIdOrderByHorarioPrevistoAsc(Long paradaId);

    boolean existsByParadaIdAndHorarioPrevisto(Long paradaId, LocalTime horarioPrevisto);
}
