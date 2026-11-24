package com.waypoint.project_tcc_app.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioParadaRequest(
        @NotNull
        LocalTime horarioPrevisto
) {
}
