(function () {
    const linhaSelect = document.getElementById('linhaSelect');
    const lista = document.getElementById('ocorrenciasLista');
    let linhas = [];

    document.addEventListener('DOMContentLoaded', async () => {
        document.getElementById('btnAtualizarOcorrencias')?.addEventListener('click', carregarOcorrencias);
        linhaSelect?.addEventListener('change', carregarOcorrencias);
        await carregarLinhas();
        await carregarOcorrencias();
    });

    async function carregarLinhas() {
        await WayPoint.executar(async () => {
            linhas = await WayPoint.get('/api/linhas');
            WayPoint.popularSelectLinhas(linhaSelect, linhas, true);
        });
    }

    async function carregarOcorrencias() {
        await WayPoint.executar(async () => {
            WayPoint.mostrarCarregando(lista, 'Carregando ocorrências');
            const linhaId = linhaSelect.value;
            const endpoint = linhaId ? `/api/linhas/${linhaId}/ocorrencias?ativa=true` : '/api/ocorrencias?ativa=true';
            render(await WayPoint.get(endpoint));
        });
    }

    function render(ocorrencias) {
        if (!ocorrencias.length) {
            lista.innerHTML = '<div class="empty">Nenhuma ocorrência ativa encontrada.</div>';
            return;
        }

        lista.innerHTML = `<div class="feed-list">${ocorrencias.map((ocorrencia) => `
            <article class="feed-card occurrence-card">
                <div class="feed-card-main">
                    <p class="occurrence-kind">${WayPoint.escapeHtml(textoTipoOcorrencia(ocorrencia.tipo))}</p>
                    <strong>${WayPoint.escapeHtml(ocorrencia.linhaCodigo)} - ${WayPoint.escapeHtml(ocorrencia.linhaNome)}</strong>
                    <div class="occurrence-meta">
                        <span class="badge ${classePrioridade(ocorrencia.prioridade)}">Prioridade: ${WayPoint.escapeHtml(textoPrioridade(ocorrencia.prioridade))}</span>
                        <span class="muted">${WayPoint.formatarData(ocorrencia.dataHora)}</span>
                    </div>
                    <p>${WayPoint.escapeHtml(ocorrencia.descricao || 'Sem descrição informada.')}</p>
                </div>
            </article>
        `).join('')}</div>`;
    }

    function textoTipoOcorrencia(tipo) {
        const textos = {
            ATRASO: 'Atraso',
            DESVIO: 'Desvio',
            VEICULO_QUEBRADO: 'Veículo quebrado',
            LOTACAO: 'Lotação',
            INTERROMPIDA: 'Linha interrompida',
            UNIFICADA: 'Linha unificada'
        };

        return textos[tipo] || tipo || 'Ocorrência';
    }

    function textoPrioridade(prioridade) {
        const textos = {
            BAIXA: 'Baixa',
            MEDIA: 'Média',
            ALTA: 'Alta'
        };

        return textos[prioridade] || prioridade || 'Média';
    }

    function classePrioridade(prioridade) {
        if (prioridade === 'ALTA') {
            return 'danger';
        }

        if (prioridade === 'MEDIA') {
            return 'warning';
        }

        return 'success';
    }
})();
