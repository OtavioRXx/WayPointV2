package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.HorarioParadaRequest;
import com.waypoint.project_tcc_app.dto.HorarioParadaResponse;
import com.waypoint.project_tcc_app.dto.ParadaRequest;
import com.waypoint.project_tcc_app.dto.ParadaResponse;
import com.waypoint.project_tcc_app.model.Sentido;
import com.waypoint.project_tcc_app.service.HorarioParadaService;
import com.waypoint.project_tcc_app.service.ParadaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Paradas", description = "Consulta e administracao das paradas vinculadas as linhas.")
public class ParadaController {

    private final ParadaService paradaService;
    private final HorarioParadaService horarioParadaService;

    public ParadaController(ParadaService paradaService, HorarioParadaService horarioParadaService) {
        this.paradaService = paradaService;
        this.horarioParadaService = horarioParadaService;
    }

    @GetMapping("/paradas")
    @Operation(summary = "Listar paradas", description = "Retorna todas as paradas cadastradas.")
    public ResponseEntity<List<ParadaResponse>> listarTodas() {
        return ResponseEntity.ok(paradaService.listarTodas()
                .stream()
                .map(DtoMapper::toParadaResponse)
                .toList());
    }

    @GetMapping("/linhas/{linhaId}/paradas")
    @Operation(summary = "Listar paradas por linha", description = "Retorna as paradas de uma linha, com filtro opcional por sentido.")
    public ResponseEntity<List<ParadaResponse>> listarPorLinha(
            @PathVariable Long linhaId,
            @RequestParam(required = false) Sentido sentido
    ) {
        return ResponseEntity.ok(paradaService.listarPorLinha(linhaId, sentido)
                .stream()
                .map(DtoMapper::toParadaResponse)
                .toList());
    }

    @GetMapping("/paradas/{id}")
    @Operation(summary = "Buscar parada por ID", description = "Retorna os dados de uma parada especifica.")
    public ResponseEntity<ParadaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(DtoMapper.toParadaResponse(paradaService.buscarPorId(id)));
    }

    @GetMapping("/paradas/{paradaId}/horarios")
    @Operation(summary = "Listar horarios da parada", description = "Retorna os horarios previstos da parada em ordem crescente.")
    public ResponseEntity<List<HorarioParadaResponse>> listarHorarios(@PathVariable Long paradaId) {
        return ResponseEntity.ok(horarioParadaService.listarPorParada(paradaId)
                .stream()
                .map(DtoMapper::toHorarioParadaResponse)
                .toList());
    }

    @PostMapping("/paradas")
    @Operation(summary = "Cadastrar parada", description = "Cria uma nova parada vinculada a uma linha. Acesso restrito a ADMIN.")
    public ResponseEntity<ParadaResponse> cadastrar(@Valid @RequestBody ParadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toParadaResponse(paradaService.salvar(DtoMapper.toParada(request))));
    }

    @PostMapping("/paradas/{paradaId}/horarios")
    @Operation(summary = "Cadastrar horario da parada", description = "Cadastra um horario previsto para uma parada. Acesso restrito a ADMIN.")
    public ResponseEntity<HorarioParadaResponse> cadastrarHorario(
            @PathVariable Long paradaId,
            @Valid @RequestBody HorarioParadaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toHorarioParadaResponse(
                        horarioParadaService.salvar(paradaId, DtoMapper.toHorarioParada(request, paradaId))
                ));
    }

    @PutMapping("/paradas/{id}")
    @Operation(summary = "Atualizar parada", description = "Atualiza os dados de uma parada existente. Acesso restrito a ADMIN.")
    public ResponseEntity<ParadaResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ParadaRequest request) {
        return ResponseEntity.ok(DtoMapper.toParadaResponse(paradaService.atualizar(id, DtoMapper.toParada(request))));
    }

    @DeleteMapping("/paradas/{id}")
    @Operation(summary = "Excluir parada", description = "Remove uma parada cadastrada. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        paradaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/paradas/{paradaId}/horarios/{horarioId}")
    @Operation(summary = "Excluir horario da parada", description = "Remove um horario previsto de uma parada. Acesso restrito a ADMIN.")
    public ResponseEntity<Void> excluirHorario(@PathVariable Long paradaId, @PathVariable Long horarioId) {
        horarioParadaService.excluir(paradaId, horarioId);
        return ResponseEntity.noContent().build();
    }
}
