package com.waypoint.project_tcc_app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NotificacaoRequest(
        @NotNull
        Long usuarioId,

        Long linhaId,

        @NotBlank
        @Size(max = 100)
        String titulo,

        @NotBlank
        String mensagem,

        Boolean lida,

        @PastOrPresent
        LocalDateTime dataCriacao
) {
}
