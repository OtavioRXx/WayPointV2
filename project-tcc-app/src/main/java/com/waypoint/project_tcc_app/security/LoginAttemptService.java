package com.waypoint.project_tcc_app.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration TEMPO_BLOQUEIO = Duration.ofMinutes(5);

    private final Map<String, TentativaLogin> tentativas = new ConcurrentHashMap<>();

    public void registrarFalha(String email) {
        String chave = normalizar(email);
        if (chave.isBlank()) {
            return;
        }

        tentativas.compute(chave, (ignorado, tentativaAtual) -> {
            TentativaLogin tentativa = tentativaAtual == null || tentativaAtual.bloqueioExpirou()
                    ? new TentativaLogin(0, null)
                    : tentativaAtual;

            int totalFalhas = tentativa.falhas() + 1;
            Instant bloqueadoAte = totalFalhas >= MAX_TENTATIVAS
                    ? Instant.now().plus(TEMPO_BLOQUEIO)
                    : tentativa.bloqueadoAte();

            return new TentativaLogin(totalFalhas, bloqueadoAte);
        });
    }

    public void registrarSucesso(String email) {
        tentativas.remove(normalizar(email));
    }

    public boolean estaBloqueado(String email) {
        TentativaLogin tentativa = tentativas.get(normalizar(email));
        if (tentativa == null) {
            return false;
        }

        if (tentativa.bloqueioExpirou()) {
            tentativas.remove(normalizar(email));
            return false;
        }

        return tentativa.bloqueadoAte() != null;
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record TentativaLogin(int falhas, Instant bloqueadoAte) {

        boolean bloqueioExpirou() {
            return bloqueadoAte != null && bloqueadoAte.isBefore(Instant.now());
        }
    }
}
