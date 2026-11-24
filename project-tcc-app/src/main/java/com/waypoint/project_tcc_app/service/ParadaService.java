package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.model.Sentido;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import com.waypoint.project_tcc_app.repository.ParadaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParadaService {

    private final ParadaRepository paradaRepository;
    private final LinhaRepository linhaRepository;

    public ParadaService(ParadaRepository paradaRepository, LinhaRepository linhaRepository) {
        this.paradaRepository = paradaRepository;
        this.linhaRepository = linhaRepository;
    }

    public List<Parada> listarTodas() {
        return paradaRepository.findAll();
    }

    public List<Parada> listarPorLinha(Long linhaId, Sentido sentido) {
        validarLinhaExistente(linhaId);

        if (sentido == null) {
            return paradaRepository.findByLinhaIdOrderBySentidoAscOrdemAsc(linhaId);
        }

        return paradaRepository.findByLinhaIdAndSentidoOrderByOrdemAsc(linhaId, sentido);
    }

    public Parada buscarPorId(Long id) {
        return paradaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parada nao encontrada."));
    }

    public Parada salvar(Parada parada) {
        prepararParada(parada);
        validarOrdemDisponivel(parada);
        return paradaRepository.save(parada);
    }

    public Parada atualizar(Long id, Parada paradaAtualizada) {
        Parada parada = buscarPorId(id);

        parada.setLinha(paradaAtualizada.getLinha());
        parada.setNome(paradaAtualizada.getNome());
        parada.setLatitude(paradaAtualizada.getLatitude());
        parada.setLongitude(paradaAtualizada.getLongitude());
        parada.setOrdem(paradaAtualizada.getOrdem());
        parada.setSentido(paradaAtualizada.getSentido());

        prepararParada(parada);
        validarOrdemDisponivel(parada);

        return paradaRepository.save(parada);
    }

    public void excluir(Long id) {
        Parada parada = buscarPorId(id);
        paradaRepository.delete(parada);
    }

    private void prepararParada(Parada parada) {
        if (parada.getNome() != null) {
            parada.setNome(parada.getNome().trim());
        }

        Linha linha = buscarLinhaDaParada(parada);
        parada.setLinha(linha);
    }

    private Linha buscarLinhaDaParada(Parada parada) {
        if (parada.getLinha() == null || parada.getLinha().getId() == null) {
            throw new IllegalArgumentException("Linha deve ser informada.");
        }

        return linhaRepository.findById(parada.getLinha().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
    }

    private void validarLinhaExistente(Long linhaId) {
        if (!linhaRepository.existsById(linhaId)) {
            throw new RecursoNaoEncontradoException("Linha nao encontrada.");
        }
    }

    private void validarOrdemDisponivel(Parada parada) {
        Long linhaId = parada.getLinha().getId();
        boolean ordemEmUso = parada.getId() == null
                ? paradaRepository.existsByLinhaIdAndSentidoAndOrdem(linhaId, parada.getSentido(), parada.getOrdem())
                : paradaRepository.existsByLinhaIdAndSentidoAndOrdemAndIdNot(
                        linhaId,
                        parada.getSentido(),
                        parada.getOrdem(),
                        parada.getId()
                );

        if (ordemEmUso) {
            throw new IllegalArgumentException("Ja existe uma parada cadastrada nesta ordem para a linha e sentido informados.");
        }
    }
}
