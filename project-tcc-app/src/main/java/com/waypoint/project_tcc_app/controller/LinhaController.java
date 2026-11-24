package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.LinhaRequest;
import com.waypoint.project_tcc_app.dto.LinhaResponse;
import com.waypoint.project_tcc_app.service.LinhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/linhas")
@Tag(name = "Linhas", description = "Consulta e administracao das linhas de transporte.")
public class LinhaController {

    private final LinhaService linhaService;

    public LinhaController(LinhaService linhaService) {
        this.linhaService = linhaService;
    }

    @GetMapping
    @Operation(summary = "Listar linhas", description = "Retorna todas as linhas cadastradas no sistema.")
    public ResponseEntity<List<LinhaResponse>> listarTodas() {
        return ResponseEntity.ok(linhaService.listarTodas()
                .stream()
                .map(DtoMapper::toLinhaResponse)
                .toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar linha por ID", description = "Retorna os dados de uma linha especifica.")
    public ResponseEntity<LinhaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(DtoMapper.toLinhaResponse(linhaService.buscarPorId(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar linha", description = "Cria uma nova linha de transporte. Acesso restrito a ADMIN.")
    public ResponseEntity<LinhaResponse> cadastrar(@Valid @RequestBody LinhaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toLinhaResponse(linhaService.salvar(DtoMapper.toLinha(request))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar linha", description = "Atualiza os dados de uma linha existente. Acesso restrito a ADMIN.")
    public ResponseEntity<LinhaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody LinhaRequest request) {
        return ResponseEntity.ok(DtoMapper.toLinhaResponse(linhaService.atualizar(id, DtoMapper.toLinha(request))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir linha", description = "Remove uma linha cadastrada. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        linhaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
