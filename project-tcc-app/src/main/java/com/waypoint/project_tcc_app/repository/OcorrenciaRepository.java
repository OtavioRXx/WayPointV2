package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    List<Ocorrencia> findAllByOrderByDataHoraDesc();

    List<Ocorrencia> findByAtivaOrderByDataHoraDesc(Boolean ativa);

    List<Ocorrencia> findByLinhaIdOrderByDataHoraDesc(Long linhaId);

    List<Ocorrencia> findByLinhaIdAndAtivaOrderByDataHoraDesc(Long linhaId, Boolean ativa);

    Optional<Ocorrencia> findFirstByLinhaIdAndAtivaTrueOrderByDataHoraDesc(Long linhaId);

    List<Ocorrencia> findByAtivaFalseAndDataEncerramentoBefore(LocalDateTime dataEncerramento);
}
