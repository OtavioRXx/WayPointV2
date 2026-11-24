package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.TipoReporte;
import jakarta.validation.constraints.NotNull;

public record ReporteParadaRequest(
        @NotNull
        Long paradaId,

        @NotNull
        TipoReporte tipoReporte
) {
}
