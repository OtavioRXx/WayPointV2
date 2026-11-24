package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.StatusLinha;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinhaRequest(
        @NotBlank
        @Size(max = 20)
        String codigo,

        @NotBlank
        @Size(max = 100)
        String nome,

        StatusLinha status
) {
}
