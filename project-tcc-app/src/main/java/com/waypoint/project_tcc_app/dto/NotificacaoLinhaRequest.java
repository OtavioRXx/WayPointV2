package com.waypoint.project_tcc_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificacaoLinhaRequest(
        Long linhaId,

        Boolean enviarParaTodos,

        @NotBlank
        @Size(max = 100)
        String titulo,

        @NotBlank
        String mensagem,

        Boolean superarNotificacoesAtuaisDaLinha
) {
}
