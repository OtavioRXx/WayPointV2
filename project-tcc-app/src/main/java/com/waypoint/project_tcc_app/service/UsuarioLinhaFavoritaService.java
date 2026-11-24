package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.User;
import com.waypoint.project_tcc_app.model.UsuarioLinhaFavorita;
import com.waypoint.project_tcc_app.model.UsuarioLinhaFavoritaId;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.UserRepository;
import com.waypoint.project_tcc_app.repository.UsuarioLinhaFavoritaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioLinhaFavoritaService {

    private final UsuarioLinhaFavoritaRepository usuarioLinhaFavoritaRepository;
    private final UserRepository userRepository;
    private final LinhaRepository linhaRepository;
    private final NotificacaoService notificacaoService;

    public UsuarioLinhaFavoritaService(
            UsuarioLinhaFavoritaRepository usuarioLinhaFavoritaRepository,
            UserRepository userRepository,
            LinhaRepository linhaRepository,
            NotificacaoService notificacaoService
    ) {
        this.usuarioLinhaFavoritaRepository = usuarioLinhaFavoritaRepository;
        this.userRepository = userRepository;
        this.linhaRepository = linhaRepository;
        this.notificacaoService = notificacaoService;
    }

    public List<UsuarioLinhaFavorita> listarPorUsuario(Long usuarioId) {
        validarUsuarioExistente(usuarioId);
        return usuarioLinhaFavoritaRepository.findByUsuarioIdOrderByLinhaNomeAsc(usuarioId);
    }

    public UsuarioLinhaFavorita favoritar(Long usuarioId, Long linhaId) {
        User usuario = buscarUsuario(usuarioId);
        Linha linha = buscarLinha(linhaId);
        UsuarioLinhaFavoritaId id = new UsuarioLinhaFavoritaId(usuarioId, linhaId);

        if (usuarioLinhaFavoritaRepository.existsById(id)) {
            throw new IllegalArgumentException("Linha ja esta favoritada por este usuario.");
        }

        UsuarioLinhaFavorita favorito = new UsuarioLinhaFavorita();
        favorito.setId(id);
        favorito.setUsuario(usuario);
        favorito.setLinha(linha);

        UsuarioLinhaFavorita salvo = usuarioLinhaFavoritaRepository.save(favorito);
        notificacaoService.sincronizarAvisosParaUsuario(usuarioId);
        return salvo;
    }

    public void desfavoritar(Long usuarioId, Long linhaId) {
        UsuarioLinhaFavoritaId id = new UsuarioLinhaFavoritaId(usuarioId, linhaId);

        if (!usuarioLinhaFavoritaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Linha favorita nao encontrada.");
        }

        usuarioLinhaFavoritaRepository.deleteById(id);
    }

    private User buscarUsuario(Long usuarioId) {
        return userRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
    }

    private Linha buscarLinha(Long linhaId) {
        return linhaRepository.findById(linhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
    }

    private void validarUsuarioExistente(Long usuarioId) {
        if (!userRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Usuario nao encontrado.");
        }
    }
}
