(function () {
    const autenticado = document.body.dataset.authenticated === 'true' && Number(document.body.dataset.userId) > 0;
    const linhaSelect = document.getElementById('linhaSelect');
    const tipoSelect = document.getElementById('tipoSelect');
    const feed = document.getElementById('reportesFeed');
    let reportes = [];

    document.addEventListener('DOMContentLoaded', async () => {
        document.getElementById('btnAtualizarReportes')?.addEventListener('click', carregarFeed);
        linhaSelect?.addEventListener('change', carregarFeed);
        tipoSelect?.addEventListener('change', render);
        await carregarLinhas();
        await carregarFeed();
    });

    async function carregarLinhas() {
        await WayPoint.executar(async () => {
            WayPoint.popularSelectLinhas(linhaSelect, await WayPoint.get('/api/linhas'), true);
        });
    }

    async function carregarFeed() {
        await WayPoint.executar(async () => {
            WayPoint.mostrarCarregando(feed, 'Carregando reportes');
            const linhaId = linhaSelect.value;
            reportes = await WayPoint.get(linhaId ? `/api/reportes/feed?linhaId=${linhaId}` : '/api/reportes/feed');
            render();
        });
    }

    function render() {
        const tipo = tipoSelect.value;
        const filtrados = tipo ? reportes.filter((reporte) => reporte.tipoReporte === tipo) : reportes;

        if (!filtrados.length) {
            feed.innerHTML = '<div class="empty">Nenhum reporte encontrado.</div>';
            return;
        }

        feed.innerHTML = filtrados.map((reporte) => `
            <article class="feed-card report-card" data-reporte-id="${reporte.reporteId}" data-tipo="${WayPoint.escapeHtml(reporte.tipoReporte)}">
                <div class="feed-card-main">
                    <div class="feed-topline">
                        <strong>${WayPoint.escapeHtml(reporte.autorNome)}</strong>
                        <time class="muted" datetime="${WayPoint.escapeHtml(reporte.dataHora)}" title="${WayPoint.escapeHtml(WayPoint.formatarData(reporte.dataHora))}">${tempoRelativo(reporte.dataHora)}</time>
                    </div>
                    <p class="report-kind">${WayPoint.escapeHtml(textoTipoReporte(reporte.tipoReporte))}</p>
                    <p class="feed-route">${WayPoint.escapeHtml(reporte.linhaCodigo)} - ${WayPoint.escapeHtml(reporte.linhaNome)}</p>
                    ${reporte.paradaNome ? `<p class="feed-stop">Parada ${WayPoint.escapeHtml(reporte.paradaNome)} - ${WayPoint.escapeHtml(reporte.paradaSentido)}</p>` : '<p class="feed-stop">Reporte vinculado somente à linha.</p>'}
                    <p class="muted">Confiabilidade ${WayPoint.formatarConfiabilidade(reporte.autorConfiabilidade)}</p>
                </div>
                <div class="feed-actions">
                    <span class="like-count" title="Confirmações">Confirmações: ${reporte.quantidadeConfirmacoes ?? 0}</span>
                    ${acaoReporte(reporte)}
                </div>
            </article>
        `).join('');

        feed.querySelectorAll('button[data-reporte-id]').forEach((botao) => {
            botao.addEventListener('click', () => confirmar(Number(botao.dataset.reporteId)));
        });
    }

    function acaoReporte(reporte) {
        if (!autenticado) {
            return '<a class="button ghost feed-confirm-button" href="/login">Entrar para confirmar</a>';
        }

        if (reporte.autorDoUsuarioAutenticado) {
            return '<span class="badge feed-own">Seu reporte</span>';
        }

        if (reporte.confirmadoPeloUsuarioAutenticado) {
            return '<button class="feed-confirm-button" type="button" disabled>Confirmado</button>';
        }

        return `<button class="success feed-confirm-button" type="button" data-reporte-id="${reporte.reporteId}">Confirmar reporte</button>`;
    }

    async function confirmar(reporteId) {
        await WayPoint.executar(async () => {
            const botao = feed.querySelector(`button[data-reporte-id="${reporteId}"]`);
            if (botao) {
                botao.disabled = true;
                botao.textContent = 'Confirmando...';
            }
            const resposta = await WayPoint.request(`/api/reportes/${reporteId}/confirmacoes`, { method: 'POST' });
            const reporte = reportes.find((item) => Number(item.reporteId) === Number(reporteId));
            if (reporte) {
                reporte.quantidadeConfirmacoes = resposta.quantidadeConfirmacoes;
                reporte.autorConfiabilidade = resposta.autorConfiabilidade;
                reporte.confirmadoPeloUsuarioAutenticado = true;
            }
            render();
            WayPoint.mostrarMensagem('Reporte confirmado com sucesso.', 'ok');
        });
    }

    function textoTipoReporte(tipoReporte) {
        const textos = {
            PASSOU: 'O ônibus passou',
            ATRASADO: 'O ônibus está atrasado',
            NAO_PASSOU: 'O ônibus ainda não passou',
            LOTADO: 'O ônibus passou lotado',
            UNIFICADA: 'A linha está unificada'
        };

        return textos[tipoReporte] || tipoReporte;
    }

    function tempoRelativo(valor) {
        const data = new Date(valor);
        if (Number.isNaN(data.getTime())) {
            return '';
        }

        const segundos = Math.max(0, Math.floor((Date.now() - data.getTime()) / 1000));
        if (segundos < 60) {
            return 'há poucos segundos';
        }

        const minutos = Math.floor(segundos / 60);
        if (minutos < 60) {
            return `há ${minutos} min`;
        }

        const horas = Math.floor(minutos / 60);
        if (horas < 24) {
            return `há ${horas} h`;
        }

        return `há ${Math.floor(horas / 24)} d`;
    }
})();
