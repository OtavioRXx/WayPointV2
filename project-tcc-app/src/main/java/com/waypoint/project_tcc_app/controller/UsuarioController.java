package com.waypoint.project_tcc_app.controller;

import com.waypoint.project_tcc_app.dto.CadastroAdminRequest;
import com.waypoint.project_tcc_app.dto.DtoMapper;
import com.waypoint.project_tcc_app.dto.UsuarioResumoResponse;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Consultas e gerenciamento administrativo de usuarios.")
public class UsuarioController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioController.class);

    private final UserService userService;

    public UsuarioController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna usuarios em formato resumido. Acesso restrito a ADMIN.")
    public ResponseEntity<List<UsuarioResumoResponse>> listarTodos() {
        return ResponseEntity.ok(userService.listarTodos()
                .stream()
                .map(DtoMapper::toUsuarioResumoResponse)
                .toList());
    }

    @PostMapping("/admins")
    @Operation(summary = "Cadastrar administrador", description = "Cria uma nova conta com perfil ADMIN. Acesso exclusivo para administradores autenticados.")
    public ResponseEntity<UsuarioResumoResponse> cadastrarAdmin(
            @Valid @RequestBody CadastroAdminRequest request,
            Authentication authentication
    ) {
        User novoAdmin = userService.cadastrarAdmin(request);
        LOGGER.info("evento=ADMIN_CRIADO criador={} novoAdmin={}",
                authentication == null ? "sistema" : authentication.getName(),
                novoAdmin.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toUsuarioResumoResponse(novoAdmin));
    }
}
