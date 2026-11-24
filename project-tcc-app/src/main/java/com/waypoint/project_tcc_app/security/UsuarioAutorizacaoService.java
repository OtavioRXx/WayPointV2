package com.waypoint.project_tcc_app.security;

import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutorizacaoService {

    private final UserRepository userRepository;

    public UsuarioAutorizacaoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validarAcessoAoUsuario(Long usuarioId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Acesso negado.");
        }

        if (isAdmin(authentication)) {
            return;
        }

        User usuarioAutenticado = userRepository.findByEmail(authentication.getName().trim().toLowerCase())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado nao encontrado."));

        if (!usuarioAutenticado.getId().equals(usuarioId)) {
            throw new AccessDeniedException("Voce nao tem permissao para acessar dados de outro usuario.");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
