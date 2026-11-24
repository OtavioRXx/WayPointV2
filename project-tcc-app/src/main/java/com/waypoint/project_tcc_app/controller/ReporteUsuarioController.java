package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.ConfirmacaoReporteResponse;
import com.waypoint.project_tcc_app.dto.ReporteParadaRequest;
import com.waypoint.project_tcc_app.dto.ReporteFeedResponse;
import com.waypoint.project_tcc_app.dto.ReporteUsuarioRequest;
import com.waypoint.project_tcc_app.dto.ReporteUsuarioResponse;
import com.waypoint.project_tcc_app.security.UsuarioAutorizacaoService;
import com.waypoint.project_tcc_app.service.ReporteUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reportes", description = "Reportes enviados por usuarios sobre a situacao das linhas.")
public class ReporteUsuarioController {

    private final ReporteUsuarioService reporteUsuarioService;
    private final UsuarioAutorizacaoService usuarioAutorizacaoService;

    public ReporteUsuarioController(
            ReporteUsuarioService reporteUsuarioService,
            UsuarioAutorizacaoService usuarioAutorizacaoService
    ) {
        this.reporteUsuarioService = reporteUsuarioService;
        this.usuarioAutorizacaoService = usuarioAutorizacaoService;
    }

    @GetMapping("/reportes")
    @Operation(summary = "Listar reportes", description = "Retorna todos os reportes cadastrados. Acesso restrito a ADMIN.")
    public ResponseEntity<List<ReporteUsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(reporteUsuarioService.listarTodos()
                .stream()
                .map(DtoMapper::toReporteUsuarioResponse)
                .toList());
    }

    @GetMapping("/reportes/{id}")
    @Operation(summary = "Buscar reporte por ID", description = "Retorna os dados de um reporte especifico. Acesso restrito a ADMIN.")
    public ResponseEntity<ReporteUsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(DtoMapper.toReporteUsuarioResponse(reporteUsuarioService.buscarPorId(id)));
    }

    @GetMapping("/reportes/feed")
    @Operation(summary = "Feed colaborativo de reportes", description = "Retorna reportes recentes com dados publicos, confirmacoes e confiabilidade do autor.")
    public ResponseEntity<List<ReporteFeedResponse>> listarFeed(
            @RequestParam(required = false) Long linhaId,
            Authentication authentication
    ) {
        String email = (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken))
                ? authentication.getName()
                : null;
        return ResponseEntity.ok(reporteUsuarioService.listarFeed(linhaId, email));
    }

    @PostMapping("/reportes/{reporteId}/confirmacoes")
    @Operation(summary = "Confirmar reporte", description = "Registra que o usuario autenticado confirma um reporte de outro usuario.")
    public ResponseEntity<ConfirmacaoReporteResponse> confirmar(
            @PathVariable Long reporteId,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reporteUsuarioService.confirmar(reporteId, authentication.getName()));
    }

    @GetMapping("/linhas/{linhaId}/reportes")
    @Operation(summary = "Listar reportes por linha", description = "Retorna os reportes associados a uma linha. Acesso restrito a ADMIN.")
    public ResponseEntity<List<ReporteUsuarioResponse>> listarPorLinha(@PathVariable Long linhaId) {
        return ResponseEntity.ok(reporteUsuarioService.listarPorLinha(linhaId)
                .stream()
                .map(DtoMapper::toReporteUsuarioResponse)
                .toList());
    }

    @GetMapping("/usuarios/{usuarioId}/reportes")
    @Operation(summary = "Listar reportes por usuario", description = "Retorna os reportes de um usuario. USER acessa apenas os proprios dados; ADMIN acessa qualquer usuario.")
    public ResponseEntity<List<ReporteUsuarioResponse>> listarPorUsuario(@PathVariable Long usuarioId) {
        usuarioAutorizacaoService.validarAcessoAoUsuario(usuarioId);
        return ResponseEntity.ok(reporteUsuarioService.listarPorUsuario(usuarioId)
                .stream()
                .map(DtoMapper::toReporteUsuarioResponse)
                .toList());
    }

    @PostMapping("/reportes")
    @Operation(summary = "Cadastrar reporte", description = "Cria um reporte de usuario para uma linha. USER pode registrar apenas em seu proprio usuario.")
    public ResponseEntity<ReporteUsuarioResponse> cadastrar(
            @Valid @RequestBody ReporteUsuarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toReporteUsuarioResponse(
                        reporteUsuarioService.salvarAutenticado(request, authentication.getName())
                ));
    }

    @PostMapping("/reportes/minhas/paradas")
    @Operation(summary = "Cadastrar reporte pela parada", description = "Cria um reporte do usuario autenticado para uma parada, inferindo a linha automaticamente.")
    public ResponseEntity<ReporteUsuarioResponse> cadastrarNaParada(
            @Valid @RequestBody ReporteParadaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toReporteUsuarioResponse(
                        reporteUsuarioService.salvarNaParada(request, authentication.getName())
                ));
    }

    @DeleteMapping("/reportes/{id}")
    @Operation(summary = "Excluir reporte", description = "Remove um reporte cadastrado. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        reporteUsuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
