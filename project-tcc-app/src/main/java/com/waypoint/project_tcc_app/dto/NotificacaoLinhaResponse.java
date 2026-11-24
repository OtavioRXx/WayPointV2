package com.waypoint.project_tcc_app.dto;

public record NotificacaoLinhaResponse(
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        int usuariosNotificados,
        String mensagem
) {
}
