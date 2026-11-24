package com.waypoint.project_tcc_app.service;

import com.waypoint.project_tcc_app.exception.RecursoNaoEncontradoException;
import com.waypoint.project_tcc_app.model.Linha;
import com.waypoint.project_tcc_app.model.StatusLinha;
import com.waypoint.project_tcc_app.repository.LinhaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LinhaService {

    private final LinhaRepository linhaRepository;

    public LinhaService(LinhaRepository linhaRepository) {
        this.linhaRepository = linhaRepository;
    }

    public List<Linha> listarTodas() {
        return linhaRepository.findAll();
    }

    public Linha buscarPorId(Long id) {
        return linhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Linha nao encontrada."));
    }

    public Linha salvar(Linha linha) {
        prepararLinha(linha);
        validarCodigoDisponivel(linha);
        return linhaRepository.save(linha);
    }

    public Linha atualizar(Long id, Linha linhaAtualizada) {
        Linha linha = buscarPorId(id);

        linha.setCodigo(linhaAtualizada.getCodigo());
        linha.setNome(linhaAtualizada.getNome());
        linha.setStatus(linhaAtualizada.getStatus());

        prepararLinha(linha);
        validarCodigoDisponivel(linha);

        return linhaRepository.save(linha);
    }

    public void excluir(Long id) {
        Linha linha = buscarPorId(id);
        linhaRepository.delete(linha);
    }

    private void prepararLinha(Linha linha) {
        if (linha.getCodigo() != null) {
            linha.setCodigo(linha.getCodigo().trim());
        }

        if (linha.getNome() != null) {
            linha.setNome(linha.getNome().trim());
        }

        if (linha.getStatus() == null) {
            linha.setStatus(StatusLinha.NORMAL);
        }
    }

    private void validarCodigoDisponivel(Linha linha) {
        boolean codigoEmUso = linha.getId() == null
                ? linhaRepository.existsByCodigo(linha.getCodigo())
                : linhaRepository.existsByCodigoAndIdNot(linha.getCodigo(), linha.getId());

        if (codigoEmUso) {
            throw new IllegalArgumentException("Ja existe uma linha cadastrada com este codigo.");
        }
    }
}
