package com.waypoint.project_tcc_app.dto;

import com.waypoint.project_tcc_app.model.TipoUsuario;

public record UsuarioResumoResponse(
        Long id,
        String nome,
        String email,
        TipoUsuario tipoUsuario
) {
}
