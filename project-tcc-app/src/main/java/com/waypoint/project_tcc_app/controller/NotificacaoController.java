package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.AvisoNotificacaoResponse;
import com.waypoint.project_tcc_app.dto.AvisoPublicoResponse;
import com.waypoint.project_tcc_app.dto.LimpezaHistoricoNotificacoesResponse;
import com.waypoint.project_tcc_app.dto.NotificacaoLinhaRequest;
import com.waypoint.project_tcc_app.dto.NotificacaoLinhaResponse;
import com.waypoint.project_tcc_app.dto.NotificacaoResponse;
import com.waypoint.project_tcc_app.model.Notificacao;
import com.waypoint.project_tcc_app.model.StatusNotificacao;
import com.waypoint.project_tcc_app.security.UsuarioAutorizacaoService;
import com.waypoint.project_tcc_app.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Notificacoes", description = "Notificacoes enviadas aos usuarios sobre linhas favoritas e ocorrencias.")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final UsuarioAutorizacaoService usuarioAutorizacaoService;

    public NotificacaoController(
            NotificacaoService notificacaoService,
            UsuarioAutorizacaoService usuarioAutorizacaoService
    ) {
        this.notificacaoService = notificacaoService;
        this.usuarioAutorizacaoService = usuarioAutorizacaoService;
    }

    @GetMapping("/usuarios/{usuarioId}/notificacoes")
    @Operation(summary = "Listar notificacoes por usuario", description = "Retorna notificacoes de um usuario, com filtro opcional por lida. USER acessa apenas os proprios dados; ADMIN acessa qualquer usuario.")
    public ResponseEntity<List<NotificacaoResponse>> listarPorUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(required = false) Boolean lida,
            @RequestParam(required = false) StatusNotificacao status
    ) {
        usuarioAutorizacaoService.validarAcessoAoUsuario(usuarioId);
        return ResponseEntity.ok(notificacaoService.listarPorUsuario(usuarioId, lida, status)
                .stream()
                .map(DtoMapper::toNotificacaoResponse)
                .toList());
    }

    @PostMapping("/notificacoes")
    @Operation(summary = "Enviar aviso oficial", description = "Cria notificacoes individuais para todos os USER ou para favoritos de uma linha. Acesso restrito a ADMIN.")
    public ResponseEntity<NotificacaoLinhaResponse> cadastrar(@Valid @RequestBody NotificacaoLinhaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacaoService.notificarUsuarios(
                        request.linhaId(),
                        request.enviarParaTodos(),
                        request.titulo(),
                        request.mensagem(),
                        request.superarNotificacoesAtuaisDaLinha()
                ));
    }

    @GetMapping("/notificacoes/publicas")
    @Operation(summary = "Listar comunicados publicos", description = "Retorna os comunicados oficiais publicados pela administracao para consulta publica.")
    public ResponseEntity<List<AvisoPublicoResponse>> listarAvisosPublicos(
            @RequestParam(required = false) StatusNotificacao status
    ) {
        return ResponseEntity.ok(notificacaoService.listarAvisosPublicos(status));
    }

    @GetMapping("/notificacoes/avisos")
    @Operation(summary = "Listar avisos oficiais", description = "Lista os comunicados oficiais publicados para administracao. Acesso restrito a ADMIN.")
    public ResponseEntity<List<AvisoNotificacaoResponse>> listarAvisosOficiais() {
        return ResponseEntity.ok(notificacaoService.listarAvisosOficiais());
    }

    @PatchMapping("/notificacoes/{id}/lida")
    @Operation(summary = "Marcar notificacao como lida", description = "Marca uma notificacao como lida. USER pode alterar apenas as proprias notificacoes.")
    public ResponseEntity<NotificacaoResponse> marcarComoLida(@PathVariable Long id) {
        Notificacao notificacao = notificacaoService.buscarPorId(id);
        usuarioAutorizacaoService.validarAcessoAoUsuario(notificacao.getUsuario().getId());
        return ResponseEntity.ok(DtoMapper.toNotificacaoResponse(notificacaoService.marcarComoLida(id)));
    }

    @PatchMapping("/notificacoes/avisos/{id}/superada")
    @Operation(summary = "Marcar aviso como superado", description = "Marca como SUPERADA o aviso oficial e todas as copias individuais associadas. Acesso restrito a ADMIN.")
    public ResponseEntity<AvisoNotificacaoResponse> marcarAvisoComoSuperado(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.marcarAvisoComoSuperado(id));
    }

    @PatchMapping("/notificacoes/avisos/oficial/{avisoId}/superada")
    @Operation(summary = "Marcar aviso oficial como superado por ID canonico", description = "Marca como SUPERADA o aviso oficial identificado por avisoId e todas as copias individuais associadas. Acesso restrito a ADMIN.")
    public ResponseEntity<AvisoNotificacaoResponse> marcarAvisoOficialComoSuperado(@PathVariable Long avisoId) {
        return ResponseEntity.ok(notificacaoService.marcarAvisoOficialComoSuperado(avisoId));
    }

    @DeleteMapping("/notificacoes/avisos/{avisoId}")
    @Operation(summary = "Excluir aviso oficial", description = "Remove definitivamente um aviso oficial, suas copias individuais e registros relacionados. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluirAviso(@PathVariable Long avisoId) {
        notificacaoService.excluirAviso(avisoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/notificacoes/minhas/historico")
    @Operation(summary = "Limpar historico de notificacoes", description = "Remove apenas notificacoes superadas do usuario autenticado.")
    public ResponseEntity<LimpezaHistoricoNotificacoesResponse> limparMeuHistorico(Authentication authentication) {
        return ResponseEntity.ok(notificacaoService.limparHistoricoDoUsuarioAutenticado(authentication.getName()));
    }

    @DeleteMapping("/notificacoes/{id}")
    @Operation(summary = "Excluir notificacao", description = "Remove uma notificacao cadastrada. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        notificacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
