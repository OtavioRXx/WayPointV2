package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.HorarioParada;
import com.waypoint.project_tcc_app.model.Parada;
import com.waypoint.project_tcc_app.repository.HorarioParadaRepository;
import com.waypoint.project_tcc_app.repository.ParadaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HorarioParadaService {

    private final HorarioParadaRepository horarioParadaRepository;
    private final ParadaRepository paradaRepository;

    public HorarioParadaService(
            HorarioParadaRepository horarioParadaRepository,
            ParadaRepository paradaRepository
    ) {
        this.horarioParadaRepository = horarioParadaRepository;
        this.paradaRepository = paradaRepository;
    }

    public List<HorarioParada> listarPorParada(Long paradaId) {
        validarParadaExistente(paradaId);
        return horarioParadaRepository.findByParadaIdOrderByHorarioPrevistoAsc(paradaId);
    }

    public HorarioParada salvar(Long paradaId, HorarioParada horarioParada) {
        Parada parada = buscarParada(paradaId);

        if (horarioParadaRepository.existsByParadaIdAndHorarioPrevisto(paradaId, horarioParada.getHorarioPrevisto())) {
            throw new IllegalArgumentException("Ja existe este horario cadastrado para a parada.");
        }

        horarioParada.setParada(parada);
        return horarioParadaRepository.save(horarioParada);
    }

    public void excluir(Long paradaId, Long horarioId) {
        HorarioParada horarioParada = horarioParadaRepository.findById(horarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Horario da parada nao encontrado."));

        if (!horarioParada.getParada().getId().equals(paradaId)) {
            throw new IllegalArgumentException("Horario nao pertence a parada informada.");
        }

        horarioParadaRepository.delete(horarioParada);
    }

    private void validarParadaExistente(Long paradaId) {
        if (!paradaRepository.existsById(paradaId)) {
            throw new RecursoNaoEncontradoException("Parada nao encontrada.");
        }
    }

    private Parada buscarParada(Long paradaId) {
        return paradaRepository.findById(paradaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parada nao encontrada."));
    }
}
