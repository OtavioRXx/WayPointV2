package com.waypoint.project_tcc_app.dto;

public record UsuarioLinhaFavoritaResponse(
        Long usuarioId,
        String usuarioNome,
        Long linhaId,
        String linhaCodigo,
        String linhaNome
) {
}
