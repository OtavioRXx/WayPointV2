package com.waypoint.project_tcc_app.repository;

import com.waypoint.project_tcc_app.model.ConfirmacaoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfirmacaoReporteRepository extends JpaRepository<ConfirmacaoReporte, Long> {

    boolean existsByReporteIdAndUsuarioId(Long reporteId, Long usuarioId);

    long countByReporteId(Long reporteId);

    void deleteByReporteId(Long reporteId);
}
