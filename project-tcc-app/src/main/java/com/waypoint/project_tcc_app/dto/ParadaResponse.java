package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.Sentido;

import java.math.BigDecimal;

public record ParadaResponse(
        Long id,
        Long linhaId,
        String linhaCodigo,
        String linhaNome,
        String nome,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer ordem,
        Sentido sentido
) {
}
