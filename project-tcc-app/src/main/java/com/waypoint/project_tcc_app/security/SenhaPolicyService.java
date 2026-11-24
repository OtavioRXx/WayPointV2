package com.waypoint.project_tcc_app.security;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class SenhaPolicyService {

    private static final Set<String> SENHAS_COMUNS = Set.of(
            "123456",
            "12345678",
            "password",
            "senha123",
            "qwerty"
    );

    public void validar(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        }

        if (SENHAS_COMUNS.contains(senha.trim().toLowerCase())) {
            throw new IllegalArgumentException("Use uma senha menos comum.");
        }
    }
}
