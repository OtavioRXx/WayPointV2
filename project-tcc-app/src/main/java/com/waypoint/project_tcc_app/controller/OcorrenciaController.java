package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.OcorrenciaRequest;
import com.waypoint.project_tcc_app.dto.OcorrenciaResponse;
import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.Ocorrencia;
import com.waypoint.project_tcc_app.service.OcorrenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Ocorrencias", description = "Registro e acompanhamento de ocorrencias que afetam linhas de transporte.")
public class OcorrenciaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcorrenciaController.class);

    private final OcorrenciaService ocorrenciaService;

    public OcorrenciaController(OcorrenciaService ocorrenciaService) {
        this.ocorrenciaService = ocorrenciaService;
    }

    @GetMapping("/ocorrencias")
    @Operation(summary = "Listar ocorrencias", description = "Retorna ocorrencias registradas, com filtro opcional por ocorrencias ativas.")
    public ResponseEntity<List<OcorrenciaResponse>> listarTodas(
            @RequestParam(required = false) Boolean ativa,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ocorrenciaService.listarTodas(filtroAtivaPara(authentication, ativa))
                .stream()
                .map(DtoMapper::toOcorrenciaResponse)
                .toList());
    }

    @GetMapping("/linhas/{linhaId}/ocorrencias")
    @Operation(summary = "Listar ocorrencias por linha", description = "Retorna ocorrencias de uma linha, com filtro opcional por ocorrencias ativas.")
    public ResponseEntity<List<OcorrenciaResponse>> listarPorLinha(
            @PathVariable Long linhaId,
            @RequestParam(required = false) Boolean ativa,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ocorrenciaService.listarPorLinha(linhaId, filtroAtivaPara(authentication, ativa))
                .stream()
                .map(DtoMapper::toOcorrenciaResponse)
                .toList());
    }

    @GetMapping("/ocorrencias/{id}")
    @Operation(summary = "Buscar ocorrencia por ID", description = "Retorna os dados de uma ocorrencia especifica.")
    public ResponseEntity<OcorrenciaResponse> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Ocorrencia ocorrencia = ocorrenciaService.buscarPorId(id);
        if (!ehAdmin(authentication) && !Boolean.TRUE.equals(ocorrencia.getAtiva())) {
            throw new RecursoNaoEncontradoException("Ocorrencia nao encontrada.");
        }

        return ResponseEntity.ok(DtoMapper.toOcorrenciaResponse(ocorrencia));
    }

    @PostMapping("/ocorrencias")
    @Operation(summary = "Cadastrar ocorrencia", description = "Cria uma ocorrencia e atualiza o status da linha afetada. Acesso restrito a ADMIN.")
    public ResponseEntity<OcorrenciaResponse> cadastrar(
            @Valid @RequestBody OcorrenciaRequest request,
            Authentication authentication
    ) {
        Ocorrencia ocorrencia = ocorrenciaService.salvar(DtoMapper.toOcorrencia(request));
        LOGGER.info("evento=OCORRENCIA_CRIADA usuario={} ocorrenciaId={} linhaId={}",
                authentication.getName(),
                ocorrencia.getId(),
                ocorrencia.getLinha().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toOcorrenciaResponse(ocorrencia));
    }

    @PutMapping("/ocorrencias/{id}")
    @Operation(summary = "Atualizar ocorrencia", description = "Atualiza uma ocorrencia existente e recalcula o status da linha. Acesso restrito a ADMIN.")
    public ResponseEntity<OcorrenciaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody OcorrenciaRequest request) {
        return ResponseEntity.ok(DtoMapper.toOcorrenciaResponse(ocorrenciaService.atualizar(id, DtoMapper.toOcorrencia(request))));
    }

    @PatchMapping("/ocorrencias/{id}/encerrar")
    @Operation(summary = "Encerrar ocorrencia", description = "Marca uma ocorrencia como inativa e recalcula o status da linha. Acesso restrito a ADMIN.")
    public ResponseEntity<OcorrenciaResponse> encerrar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Ocorrencia ocorrencia = ocorrenciaService.encerrar(id);
        LOGGER.info("evento=OCORRENCIA_ENCERRADA usuario={} ocorrenciaId={} linhaId={}",
                authentication.getName(),
                ocorrencia.getId(),
                ocorrencia.getLinha().getId());
        return ResponseEntity.ok(DtoMapper.toOcorrenciaResponse(ocorrencia));
    }

    @DeleteMapping("/ocorrencias/{id}")
    @Operation(summary = "Excluir ocorrencia", description = "Remove uma ocorrencia e recalcula o status da linha. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ocorrenciaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private Boolean filtroAtivaPara(Authentication authentication, Boolean ativaSolicitada) {
        if (ehAdmin(authentication)) {
            return ativaSolicitada;
        }

        return true;
    }

    private boolean ehAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));
    }
}
