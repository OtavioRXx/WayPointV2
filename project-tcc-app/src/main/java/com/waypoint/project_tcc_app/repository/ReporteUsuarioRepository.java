package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.ReporteUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteUsuarioRepository extends JpaRepository<ReporteUsuario, Long> {

    List<ReporteUsuario> findAllByOrderByDataHoraDesc();

    List<ReporteUsuario> findByLinhaIdOrderByDataHoraDesc(Long linhaId);

    List<ReporteUsuario> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId);

    Optional<ReporteUsuario> findTopByUsuarioIdAndParadaIdOrderByDataHoraDesc(Long usuarioId, Long paradaId);

    Optional<ReporteUsuario> findTopByUsuarioIdAndLinhaIdAndParadaIsNullOrderByDataHoraDesc(Long usuarioId, Long linhaId);

    List<ReporteUsuario> findByDataHoraAfterOrderByDataHoraDesc(LocalDateTime dataHora);

    List<ReporteUsuario> findByLinhaIdAndDataHoraAfterOrderByDataHoraDesc(Long linhaId, LocalDateTime dataHora);

    List<ReporteUsuario> findByDataHoraBefore(LocalDateTime dataHora);
}
