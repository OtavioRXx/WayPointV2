package com.waypoint.project_tcc_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastroAdminRequest(
        @NotBlank
        @Size(max = 100)
        String nome,

        @NotBlank
        @Size(min = 11, max = 14)
        String cpf,

        @NotNull
        @Past
        LocalDate dataNascimento,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 8, max = 255)
        String senha
) {
}
