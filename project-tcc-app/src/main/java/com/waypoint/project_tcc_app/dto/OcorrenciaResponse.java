package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.Prioridade;

import java.time.LocalDateTime;

public record OcorrenciaResponse(
        Long id,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        String tipo,
        String descricao,
        Prioridade prioridade,
        Boolean ativa,
        LocalDateTime dataHora,
        LocalDateTime dataEncerramento
) {
}
