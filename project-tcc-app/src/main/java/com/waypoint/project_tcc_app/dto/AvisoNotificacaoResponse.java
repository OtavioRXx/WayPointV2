package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.model.TipoNotificacao;
import com.waypoint.project_tcc_app.model.Prioridade;

import java.time.LocalDateTime;

public record AvisoNotificacaoResponse(
        Long avisoId,
        Long referenciaNotificacaoId,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        Long ocorrenciaId,
        TipoNotificacao tipoNotificacao,
        String tipoOcorrencia,
        Prioridade prioridadeOcorrencia,
        String titulo,
        String mensagem,
        StatusNotificacao status,
        int usuariosAlcancados,
        LocalDateTime dataCriacao
) {
}
