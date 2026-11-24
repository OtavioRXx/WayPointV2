package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.Prioridade;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.model.TipoNotificacao;

import java.time.LocalDateTime;

public record AvisoPublicoResponse(
        Long id,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        Long ocorrenciaId,
        TipoNotificacao tipoNotificacao,
        String tipoOcorrencia,
        Prioridade prioridadeOcorrencia,
        Boolean enviarParaTodos,
        String titulo,
        String mensagem,
        StatusNotificacao status,
        LocalDateTime dataCriacao
) {
}
