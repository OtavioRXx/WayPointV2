package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.UsuarioLinhaFavoritaResponse;
import com.waypoint.project_tcc_app.security.UsuarioAutorizacaoService;
import com.waypoint.project_tcc_app.service.UsuarioLinhaFavoritaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/linhas-favoritas")
@Tag(name = "Linhas favoritas", description = "Controle das linhas favoritas de cada usuario.")
public class UsuarioLinhaFavoritaController {

    private final UsuarioLinhaFavoritaService usuarioLinhaFavoritaService;
    private final UsuarioAutorizacaoService usuarioAutorizacaoService;

    public UsuarioLinhaFavoritaController(
            UsuarioLinhaFavoritaService usuarioLinhaFavoritaService,
            UsuarioAutorizacaoService usuarioAutorizacaoService
    ) {
        this.usuarioLinhaFavoritaService = usuarioLinhaFavoritaService;
        this.usuarioAutorizacaoService = usuarioAutorizacaoService;
    }

    @GetMapping
    @Operation(summary = "Listar linhas favoritas", description = "Retorna as linhas favoritas de um usuario. USER acessa apenas os proprios dados; ADMIN acessa qualquer usuario.")
    public ResponseEntity<List<UsuarioLinhaFavoritaResponse>> listarPorUsuario(@PathVariable Long usuarioId) {
        usuarioAutorizacaoService.validarAcessoAoUsuario(usuarioId);
        return ResponseEntity.ok(usuarioLinhaFavoritaService.listarPorUsuario(usuarioId)
                .stream()
                .map(DtoMapper::toUsuarioLinhaFavoritaResponse)
                .toList());
    }

    @PostMapping("/{linhaId}")
    @Operation(summary = "Favoritar linha", description = "Adiciona uma linha aos favoritos do usuario autenticado.")
    public ResponseEntity<UsuarioLinhaFavoritaResponse> favoritar(@PathVariable Long usuarioId, @PathVariable Long linhaId) {
        usuarioAutorizacaoService.validarAcessoAoUsuario(usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toUsuarioLinhaFavoritaResponse(usuarioLinhaFavoritaService.favoritar(usuarioId, linhaId)));
    }

    @DeleteMapping("/{linhaId}")
    @Operation(summary = "Desfavoritar linha", description = "Remove uma linha dos favoritos do usuario autenticado.")
    public ResponseEntity<Void> desfavoritar(@PathVariable Long usuarioId, @PathVariable Long linhaId) {
        usuarioAutorizacaoService.validarAcessoAoUsuario(usuarioId);
        usuarioLinhaFavoritaService.desfavoritar(usuarioId, linhaId);
        return ResponseEntity.noContent().build();
    }
}
