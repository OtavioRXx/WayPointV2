package com.waypoint.project_tcc_app.dto;

import java.time.LocalTime;

public record HorarioParadaResponse(
        Long id,
        Long paradaId,
        String paradaNome,
        LocalTime horarioPrevisto
) {
}
