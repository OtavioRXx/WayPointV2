package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.TipoReporte;

import java.time.LocalDateTime;

public record ReporteUsuarioResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        Long paradaId,
        String paradaNome,
        String paradaSentido,
        TipoReporte tipoReporte,
        LocalDateTime dataHora
) {
}
