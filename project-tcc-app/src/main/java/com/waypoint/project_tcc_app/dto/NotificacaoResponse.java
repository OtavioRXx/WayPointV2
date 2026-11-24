package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.model.TipoNotificacao;
import com.waypoint.project_tcc_app.model.Prioridade;

import java.time.LocalDateTime;

public record NotificacaoResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        Long ocorrenciaId,
        TipoNotificacao tipoNotificacao,
        String tipoOcorrencia,
        Prioridade prioridadeOcorrencia,
        String titulo,
        String mensagem,
        Boolean lida,
        StatusNotificacao status,
        LocalDateTime dataCriacao
) {
}
