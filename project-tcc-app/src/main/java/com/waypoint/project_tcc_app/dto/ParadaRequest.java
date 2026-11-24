package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.Sentido;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ParadaRequest(
        @NotNull
        Long linhaId,

        @NotBlank
        @Size(max = 150)
        String nome,

        @NotNull
        @DecimalMin("-90.00000000")
        @DecimalMax("90.00000000")
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.00000000")
        @DecimalMax("180.00000000")
        BigDecimal longitude,

        @NotNull
        @Min(1)
        Integer ordem,

        @NotNull
        Sentido sentido
) {
}
