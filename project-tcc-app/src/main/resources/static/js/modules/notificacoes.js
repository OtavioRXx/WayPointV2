(function () {
    const autenticado = document.body.dataset.authenticated === 'true';
    const usuarioId = Number(document.body.dataset.userId);
    const podeAutenticarApi = autenticado && usuarioId > 0;
    const filtroLida = document.getElementById('filtroLida');
    const lista = document.getElementById('notificacoesLista');

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('btnAtualizarNotificacoes')?.addEventListener('click', carregar);
        document.getElementById('btnLimparHistorico')?.addEventListener('click', limparHistorico);
        filtroLida?.addEventListener('change', carregar);
        carregar();
    });

    async function carregar() {
        await WayPoint.executar(async () => {
            WayPoint.mostrarCarregando(lista, 'Carregando notificações');
            if (podeAutenticarApi) {
                const valorFiltro = filtroLida ? filtroLida.value : '';
                const filtro = valorFiltro ? `&lida=${encodeURIComponent(valorFiltro)}` : '';
                const atuais = await WayPoint.get(`/api/usuarios/${usuarioId}/notificacoes?status=ATIVA${filtro}`);
                const historico = await WayPoint.get(`/api/usuarios/${usuarioId}/notificacoes?status=SUPERADA${filtro}`);
                render(atuais, historico, true);
            } else {
                const atuais = await WayPoint.get('/api/notificacoes/publicas?status=ATIVA');
                const historico = await WayPoint.get('/api/notificacoes/publicas?status=SUPERADA');
                render(atuais, historico, false);
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
        });
    }

    function render(atuais, historico, ehAutenticado) {
        lista.innerHTML = `
            <div class="card">
                <h3>${ehAutenticado ? 'Atuais' : 'Comunicados ativos'}</h3>
                ${listaNotificacoes(atuais, ehAutenticado ? 'Nenhuma notificação atual.' : 'Nenhum comunicado oficial ativo no momento.', ehAutenticado)}
            </div>
            <div class="card">
                <h3>${ehAutenticado ? 'Histórico' : 'Histórico de comunicados'}</h3>
                ${listaNotificacoes(historico, ehAutenticado ? 'Nenhuma notificação no histórico.' : 'Nenhum comunicado no histórico.', ehAutenticado)}
            </div>
        `;

        if (ehAutenticado) {
            lista.querySelectorAll('button[data-notificacao-id]').forEach((botao) => {
                botao.addEventListener('click', () => marcarLida(Number(botao.dataset.notificacaoId)));
            });
        }
    }

    function listaNotificacoes(notificacoes, vazio, ehAutenticado) {
        if (!Array.isArray(notificacoes) || !notificacoes.length) {
            return `<div class="empty">${vazio}</div>`;
        }

        return `<div class="feed-list">${notificacoes.map((notificacao) => `
            <article class="feed-card notification-card ${ehAutenticado ? (notificacao.lida ? 'read' : 'unread') : (notificacao.status === 'ATIVA' ? 'unread' : 'read')}" data-notificacao-card-id="${notificacao.id}">
                <div class="feed-card-main">
                    <span class="badge ${notificacao.tipoNotificacao === 'OCORRENCIA' ? 'warning' : ''}">${WayPoint.escapeHtml(textoTipoNotificacao(notificacao))}</span>
                    <strong>${WayPoint.escapeHtml(notificacao.titulo)}</strong>
                    <p>${WayPoint.escapeHtml(notificacao.mensagem)}</p>
                    <p class="muted">${textoLinha(notificacao)} | ${WayPoint.escapeHtml(notificacao.status)} | ${WayPoint.formatarData(notificacao.dataCriacao)}</p>
                    ${notificacao.tipoNotificacao === 'OCORRENCIA' ? `<p class="muted">Tipo: ${WayPoint.escapeHtml(textoTipoOcorrencia(notificacao.tipoOcorrencia))} | Prioridade: ${WayPoint.escapeHtml(textoPrioridade(notificacao.prioridadeOcorrencia))}</p>` : ''}
                </div>
                <div class="feed-actions">
                    ${ehAutenticado
                            ? `<span class="badge ${notificacao.lida ? '' : 'success'}">${notificacao.lida ? 'Lida' : 'Não lida'}</span>
                               ${!notificacao.lida && notificacao.status === 'ATIVA' ? `<button class="success" type="button" data-notificacao-id="${notificacao.id}">Marcar lida</button>` : ''}`
                            : `<span class="badge ${notificacao.status === 'ATIVA' ? 'success' : ''}">${notificacao.status === 'ATIVA' ? 'Vigente' : 'Encerrado'}</span>`}
                </div>
            </article>
        `).join('')}</div>`;
    }

    function textoTipoNotificacao(notificacao) {
        return notificacao.tipoNotificacao === 'OCORRENCIA' ? 'OCORRÊNCIA' : 'AVISO';
    }

    function textoLinha(notificacao) {
        if (!notificacao.linhaId) {
            return 'Aviso geral';
        }

        return `${WayPoint.escapeHtml(notificacao.linhaCodigo || '')} ${WayPoint.escapeHtml(notificacao.linhaNome || '')}`.trim();
    }

    function textoTipoOcorrencia(tipo) {
        const textos = {
            ATRASO: 'Atraso',
            DESVIO: 'Desvio',
            VEICULO_QUEBRADO: 'Ônibus quebrado',
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

    async function marcarLida(id) {
        await WayPoint.executar(async () => {
            lista.querySelector(`[data-notificacao-card-id="${id}"]`)?.classList.add('fade-out');
            await WayPoint.request(`/api/notificacoes/${id}/lida`, { method: 'PATCH' });
            await carregar();
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            WayPoint.mostrarMensagem('Notificação marcada como lida.', 'ok');
        });
    }

    async function limparHistorico() {
        if (!confirm('Deseja realmente limpar seu histórico de notificações?')) {
            return;
        }

        await WayPoint.executar(async () => {
            const resposta = await WayPoint.request('/api/notificacoes/minhas/historico', { method: 'DELETE' });
            await carregar();
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            WayPoint.mostrarMensagem(`${resposta.mensagem} Removidas: ${resposta.notificacoesRemovidas}.`, 'ok');
        });
    }
})();
