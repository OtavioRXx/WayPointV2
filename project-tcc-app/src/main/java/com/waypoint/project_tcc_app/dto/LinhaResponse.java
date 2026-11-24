package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.StatusLinha;

public record LinhaResponse(
        Long id,
        String codigo,
        String nome,
        StatusLinha status
) {
}
