package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.TipoReporte;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record ReporteUsuarioRequest(
        Long usuarioId,

        @NotNull
        Long linhaId,

        Long paradaId,

        @NotNull
        TipoReporte tipoReporte,

        @PastOrPresent
        LocalDateTime dataHora
) {
}
