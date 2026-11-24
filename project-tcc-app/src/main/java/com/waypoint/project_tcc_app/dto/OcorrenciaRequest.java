package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record OcorrenciaRequest(
        @NotNull
        Long linhaId,

        @NotBlank
        @Size(max = 50)
        String tipo,

        String descricao,

        Prioridade prioridade,

        Boolean ativa,

        @PastOrPresent
        LocalDateTime dataHora
) {
}
