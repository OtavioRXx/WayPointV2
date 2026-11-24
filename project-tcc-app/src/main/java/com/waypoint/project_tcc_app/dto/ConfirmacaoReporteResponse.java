package com.waypoint.project_tcc_app.dto;

import java.math.BigDecimal;

public record ConfirmacaoReporteResponse(
        Long reporteId,
        Long usuarioConfirmacaoId,
        long quantidadeConfirmacoes,
        BigDecimal autorConfiabilidade,
        String mensagem
) {
}
