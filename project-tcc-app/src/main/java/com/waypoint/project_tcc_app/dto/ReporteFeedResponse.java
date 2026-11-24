package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.TipoReporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReporteFeedResponse(
        Long reporteId,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        Long paradaId,
        String paradaNome,
        String paradaSentido,
        TipoReporte tipoReporte,
        LocalDateTime dataHora,
        String autorNome,
        BigDecimal autorConfiabilidade,
        long quantidadeConfirmacoes,
        boolean confirmadoPeloUsuarioAutenticado,
        boolean autorDoUsuarioAutenticado
) {
}
