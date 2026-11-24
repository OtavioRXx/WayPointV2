/*
 * WayPoint — painel reformulado da página inicial.
 * Compatível com visitantes sem login, passageiros autenticados e administradores.
 */
(function () {
    const autenticado = document.body.dataset.authenticated === 'true';
    const usuarioId = Number(document.body.dataset.userId);
    const podeAutenticarApi = autenticado && usuarioId > 0;

    const situacao = document.getElementById('situacaoLinhas');
    const spotlight = document.getElementById('spotlightComunicados');
    const resumoNotificacoes = document.getElementById('resumoNotificacoes');
    const resumoReportes = document.getElementById('resumoReportes');
    const buscaInput = document.getElementById('homeBuscaLinha');

    const kpiLinhasNormais = document.getElementById('kpiLinhasNormais');
    const kpiLinhasDetalhe = document.getElementById('kpiLinhasDetalhe');
    const kpiNotificacoesCard = document.getElementById('kpiNotificacoesCard');
    const kpiNotificacoesValor = document.getElementById('kpiNotificacoesValor');
    const kpiNotificacoesDetalhe = document.getElementById('kpiNotificacoesDetalhe');
    const kpiReportesValor = document.getElementById('kpiReportesValor');
    const kpiReportesDetalhe = document.getElementById('kpiReportesDetalhe');
    const kpiFavoritasValor = document.getElementById('kpiFavoritasValor');
    const kpiFavoritasDetalhe = document.getElementById('kpiFavoritasDetalhe');

    const TEXTO_STATUS = {
        NORMAL: 'Normal',
        ATRASADA: 'Atrasada',
        LOTADA: 'Lotada',
        INTERROMPIDA: 'Interrompida',
        UNIFICADA: 'Unificada'
    };

    const TEXTO_REPORTE = {
        PASSOU: 'O ônibus passou',
        ATRASADO: 'O ônibus está atrasado',
        NAO_PASSOU: 'O ônibus ainda não passou',
        LOTADO: 'O ônibus passou lotado',
        UNIFICADA: 'A linha está unificada'
    };

    let linhasCache = [];
    let favoritosCache = new Set();
    let filtroAtual = 'todas';
    let termoBusca = '';

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('btnAtualizarInicio')?.addEventListener('click', carregar);

        buscaInput?.addEventListener('input', () => {
            termoBusca = (buscaInput.value || '').trim().toLowerCase();
            renderSituacao();
        });

        document.querySelectorAll('[data-home-filter]').forEach((botao) => {
            botao.addEventListener('click', () => {
                filtroAtual = botao.dataset.homeFilter || 'todas';
                document.querySelectorAll('[data-home-filter]').forEach((item) => {
                    item.classList.toggle('active', item === botao);
                });
                renderSituacao();
            });
        });

        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                carregarNotificacoes(favoritosCache);
            }
        });

        window.setInterval(() => {
            if (document.visibilityState === 'visible') {
                carregarNotificacoes(favoritosCache);
            }
        }, 45000);

        carregar();
    });

    async function carregar() {
        const favoritos = await carregarFavoritos();
        favoritosCache = favoritos || new Set();
        atualizarKpiFavoritas(favoritosCache);

        await Promise.allSettled([
            carregarSituacao(),
            carregarNotificacoes(favoritosCache),
            carregarReportes()
        ]);

        window.WayPointNav?.atualizarIndicadorNotificacoes();
    }

    async function carregarFavoritos() {
        if (!podeAutenticarApi) {
            return new Set();
        }

        try {
            const resposta = await WayPoint.get(`/api/usuarios/${usuarioId}/linhas-favoritas`);
            return new Set((resposta || []).map((favorito) => Number(favorito.linhaId)));
        } catch (erro) {
            console.debug('Favoritos indisponíveis no painel inicial.', erro);
            return new Set();
        }
    }

    function atualizarKpiFavoritas(favoritos) {
        if (!kpiFavoritasValor) {
            return;
        }
        const total = favoritos ? favoritos.size : 0;
        kpiFavoritasValor.textContent = String(total);
        if (kpiFavoritasDetalhe) {
            kpiFavoritasDetalhe.textContent = total === 0
                    ? 'Favorite em Linhas'
                    : (total === 1 ? '1 linha acompanhada' : `${total} linhas acompanhadas`);
        }
    }

    async function carregarSituacao() {
        if (!situacao) {
            return;
        }

        WayPoint.mostrarCarregando(situacao, 'Carregando situação das linhas');

        try {
            const linhas = await WayPoint.get('/api/linhas');
            linhasCache = Array.isArray(linhas) ? linhas : [];
            atualizarKpiLinhas();
            renderSituacao();
        } catch (erro) {
            if (kpiLinhasNormais) {
                kpiLinhasNormais.textContent = '--';
            }
            situacao.innerHTML = `<div class="empty">Não foi possível carregar a situação das linhas. ${WayPoint.escapeHtml(erro.message || '')}</div>`;
        }
    }

    function atualizarKpiLinhas() {
        const total = linhasCache.length;
        const normais = linhasCache.filter((linha) => (linha.status || 'NORMAL') === 'NORMAL').length;
        const comAtencao = total - normais;

        if (kpiLinhasNormais) {
            kpiLinhasNormais.textContent = total ? `${normais}/${total}` : '0';
        }
        if (kpiLinhasDetalhe) {
            if (!total) {
                kpiLinhasDetalhe.textContent = 'Nenhuma linha cadastrada';
            } else if (comAtencao === 0) {
                kpiLinhasDetalhe.textContent = 'Todas em circulação normal';
            } else {
                kpiLinhasDetalhe.textContent = `${comAtencao} ${comAtencao === 1 ? 'linha pede' : 'linhas pedem'} atenção`;
            }
        }
    }

    function renderSituacao() {
        if (!situacao) {
            return;
        }

        if (!linhasCache.length) {
            situacao.innerHTML = '<div class="empty">Nenhuma linha cadastrada até o momento.</div>';
            return;
        }

        const normais = linhasCache.filter((linha) => (linha.status || 'NORMAL') === 'NORMAL').length;
        let filtradas = [...linhasCache];

        if (filtroAtual === 'atencao') {
            filtradas = filtradas.filter((linha) => (linha.status || 'NORMAL') !== 'NORMAL');
        } else if (filtroAtual === 'favoritas') {
            filtradas = filtradas.filter((linha) => favoritosCache.has(Number(linha.id)));
        }

        if (termoBusca) {
            filtradas = filtradas.filter((linha) =>
                `${linha.codigo || ''} ${linha.nome || ''}`.toLowerCase().includes(termoBusca)
            );
        }

        filtradas.sort((a, b) => {
            const favDiff = Number(favoritosCache.has(Number(b.id))) - Number(favoritosCache.has(Number(a.id)));
            if (favDiff !== 0) {
                return favDiff;
            }
            const statusDiff = Number((b.status || 'NORMAL') !== 'NORMAL') - Number((a.status || 'NORMAL') !== 'NORMAL');
            if (statusDiff !== 0) {
                return statusDiff;
            }
            return String(a.codigo || '').localeCompare(String(b.codigo || ''), 'pt-BR');
        });

        const listaHtml = filtradas.length
                ? `<div class="board">
                    ${filtradas.map((linha) => `
                        <div class="board-row">
                            <span class="line-plate">${WayPoint.escapeHtml(linha.codigo)}</span>
                            <div class="board-name">
                                <strong>${WayPoint.escapeHtml(linha.nome)}</strong>
                                ${favoritosCache.has(Number(linha.id)) ? '<span class="board-fav">Favorita</span>' : ''}
                            </div>
                            <span class="badge status-badge status-${String(linha.status || 'NORMAL').toLowerCase()}">${WayPoint.escapeHtml(TEXTO_STATUS[linha.status] || linha.status || 'Normal')}</span>
                            <a class="board-link" href="/paradas?linhaId=${encodeURIComponent(linha.id)}" aria-label="Ver a rota da linha ${WayPoint.escapeHtml(linha.codigo)} no mapa">
                                Ver no mapa
                                <svg class="icon icon-sm" aria-hidden="true"><use href="/img/icons.svg#i-chevron-right"></use></svg>
                            </a>
                        </div>
                    `).join('')}
                </div>`
                : `<div class="empty">${mensagemFiltroVazio()}</div>`;

        situacao.innerHTML = `
            <div class="status-summary">
                <strong>${normais}/${linhasCache.length}</strong>
                <span>linhas em situação normal.${normais === linhasCache.length ? '' : ` ${linhasCache.length - normais} ${linhasCache.length - normais === 1 ? 'pede' : 'pedem'} atenção agora.`}</span>
                <div class="status-meter" aria-hidden="true">
                    ${linhasCache.map((linha) => `<span class="${classeMedidor(linha.status)}"></span>`).join('')}
                </div>
            </div>
            ${listaHtml}
        `;
    }

    function mensagemFiltroVazio() {
        if (termoBusca) {
            return `Nenhuma linha encontrada para "${WayPoint.escapeHtml(termoBusca)}".`;
        }
        if (filtroAtual === 'atencao') {
            return 'Nenhuma linha com alteração neste momento. Todas seguem em situação normal.';
        }
        if (filtroAtual === 'favoritas') {
            return 'Você ainda não favoritou nenhuma linha. Acesse Linhas para marcar suas favoritas.';
        }
        return 'Nenhuma linha encontrada.';
    }

    function classeMedidor(status) {
        if (status === 'INTERROMPIDA') {
            return 'is-down';
        }
        if (status === 'ATRASADA' || status === 'LOTADA') {
            return 'is-alert';
        }
        if (status === 'UNIFICADA') {
            return 'is-info';
        }
        return '';
    }

    async function carregarNotificacoes(favoritos) {
        if (!resumoNotificacoes) {
            return;
        }

        try {
            if (podeAutenticarApi) {
                const ativas = await WayPoint.get(`/api/usuarios/${usuarioId}/notificacoes?status=ATIVA`);
                const listaAtivas = Array.isArray(ativas) ? ativas : [];
                const naoLidas = listaAtivas.filter((item) => !item.lida);
                const totalNaoLidas = naoLidas.length;
                const totalFavoritas = favoritos ? favoritos.size : 0;

                atualizarKpiNotificacoes(totalNaoLidas, true, listaAtivas.length);
                renderSpotlight(listaAtivas, true);

                const preview = listaAtivas.slice(0, 3);
                resumoNotificacoes.innerHTML = `
                    <a class="unread-count${totalNaoLidas ? ' has-items' : ''}" href="/notificacoes">
                        <strong>${totalNaoLidas}</strong>
                        <span>${totalNaoLidas === 1 ? 'notificação não lida' : 'notificações não lidas'}${totalNaoLidas ? ' aguardando sua leitura.' : '. Tudo em dia por aqui.'}</span>
                    </a>
                    ${preview.length ? `
                        <ul class="home-notice-mini">
                            ${preview.map((item) => `
                                <li class="${item.lida ? 'is-read' : 'is-unread'}">
                                    <div class="home-notice-mini-head">
                                        <span class="badge ${item.tipoNotificacao === 'OCORRENCIA' ? 'warning' : ''}">${item.tipoNotificacao === 'OCORRENCIA' ? 'Ocorrência' : (item.linhaId ? WayPoint.escapeHtml(item.linhaCodigo || 'Linha') : 'Aviso geral')}</span>
                                        <time datetime="${WayPoint.escapeHtml(item.dataCriacao)}">${tempoRelativo(item.dataCriacao)}</time>
                                    </div>
                                    <strong>${WayPoint.escapeHtml(item.titulo)}</strong>
                                </li>
                            `).join('')}
                        </ul>
                    ` : ''}
                    <p class="side-note">${textoFavoritas(totalFavoritas)}</p>
                `;
            } else {
                const publicas = await WayPoint.get('/api/notificacoes/publicas?status=ATIVA');
                const listaPublicas = Array.isArray(publicas) ? publicas : [];
                const totalAtivos = listaPublicas.length;

                atualizarKpiNotificacoes(totalAtivos, false, totalAtivos);
                renderSpotlight(listaPublicas, false);

                const preview = listaPublicas.slice(0, 3);
                resumoNotificacoes.innerHTML = `
                    <a class="unread-count${totalAtivos ? ' has-items' : ''}" href="/notificacoes">
                        <strong>${totalAtivos}</strong>
                        <span>${totalAtivos === 1 ? 'comunicado oficial ativo' : 'comunicados oficiais ativos'}${totalAtivos ? ' publicado(s) pela administração.' : ' no momento.'}</span>
                    </a>
                    ${preview.length ? `
                        <ul class="home-notice-mini">
                            ${preview.map((item) => `
                                <li class="is-unread">
                                    <div class="home-notice-mini-head">
                                        <span class="badge ${item.tipoNotificacao === 'OCORRENCIA' ? 'warning' : ''}">${item.tipoNotificacao === 'OCORRENCIA' ? 'Ocorrência' : (item.linhaId ? WayPoint.escapeHtml(item.linhaCodigo || 'Linha') : 'Aviso geral')}</span>
                                        <time datetime="${WayPoint.escapeHtml(item.dataCriacao)}">${tempoRelativo(item.dataCriacao)}</time>
                                    </div>
                                    <strong>${WayPoint.escapeHtml(item.titulo)}</strong>
                                </li>
                            `).join('')}
                        </ul>
                    ` : ''}
                    <p class="side-note">Entre na sua conta para marcar avisos como lidos e acompanhar alertas das suas linhas favoritas.</p>
                `;
            }
        } catch (erro) {
            if (spotlight) {
                spotlight.classList.add('hidden');
            }
            resumoNotificacoes.innerHTML = `<div class="empty">Não foi possível carregar os avisos. ${WayPoint.escapeHtml(erro.message || '')}</div>`;
        }
    }

    function atualizarKpiNotificacoes(total, ehAutenticado, totalAtivas) {
        if (kpiNotificacoesValor) {
            kpiNotificacoesValor.textContent = String(total);
        }
        if (kpiNotificacoesCard) {
            kpiNotificacoesCard.classList.toggle('has-alert', total > 0);
        }
        if (kpiNotificacoesDetalhe) {
            if (ehAutenticado) {
                kpiNotificacoesDetalhe.textContent = total > 0
                        ? (total === 1 ? '1 aviso não lido' : `${total} avisos não lidos`)
                        : (totalAtivas > 0 ? `${totalAtivas} ativo(s), tudo lido` : 'Nenhum aviso pendente');
            } else {
                kpiNotificacoesDetalhe.textContent = total > 0
                        ? 'Comunicados da administração'
                        : 'Nenhum comunicado ativo';
            }
        }
    }

    function renderSpotlight(avisos, ehAutenticado) {
        if (!spotlight) {
            return;
        }

        const destaques = ehAutenticado
                ? [...avisos].sort((a, b) => Number(Boolean(a.lida)) - Number(Boolean(b.lida))).slice(0, 3)
                : avisos.slice(0, 3);

        if (!destaques.length) {
            spotlight.classList.add('hidden');
            spotlight.innerHTML = '';
            return;
        }

        const naoLidasCount = ehAutenticado ? avisos.filter((item) => !item.lida).length : avisos.length;

        spotlight.classList.remove('hidden');
        spotlight.innerHTML = `
            <div class="spotlight-head">
                <div class="spotlight-title-wrap">
                    <span class="spotlight-icon" aria-hidden="true">
                        <svg class="icon" aria-hidden="true"><use href="/img/icons.svg#i-megaphone"></use></svg>
                    </span>
                    <div>
                        <h2 id="spotlightTitulo">Comunicados recentes da administração</h2>
                        <p class="muted">${ehAutenticado
                                ? (naoLidasCount > 0 ? `Você possui ${naoLidasCount} ${naoLidasCount === 1 ? 'notificação não visualizada' : 'notificações não visualizadas'}.` : 'Avisos vigentes da administração e das suas linhas.')
                                : 'Avisos públicos vigentes publicados pela administração do transporte.'}</p>
                    </div>
                </div>
                <a class="wp-button ghost" href="/notificacoes">Ver todas</a>
            </div>
            <div class="spotlight-grid">
                ${destaques.map((item) => `
                    <article class="spotlight-card ${ehAutenticado && !item.lida ? 'is-unread' : ''}">
                        <div class="spotlight-card-meta">
                            <span class="badge ${item.tipoNotificacao === 'OCORRENCIA' ? 'warning' : 'success'}">${item.tipoNotificacao === 'OCORRENCIA' ? 'Ocorrência' : (item.linhaId ? `${WayPoint.escapeHtml(item.linhaCodigo || '')}` : 'Aviso geral')}</span>
                            <time class="muted" datetime="${WayPoint.escapeHtml(item.dataCriacao)}" title="${WayPoint.escapeHtml(WayPoint.formatarData(item.dataCriacao))}">${tempoRelativo(item.dataCriacao)}</time>
                        </div>
                        <h3>${WayPoint.escapeHtml(item.titulo)}</h3>
                        <p>${WayPoint.escapeHtml(item.mensagem)}</p>
                        <div class="spotlight-card-foot">
                            <span class="muted">${item.linhaId ? `${WayPoint.escapeHtml(item.linhaCodigo || '')} - ${WayPoint.escapeHtml(item.linhaNome || '')}` : 'Para todos os passageiros'}</span>
                            ${ehAutenticado && !item.lida
                                    ? `<button type="button" class="spotlight-read-btn" data-marcar-lida-home="${item.id}">Marcar lida</button>`
                                    : (ehAutenticado ? '<span class="badge">Lida</span>' : '<span class="badge">Vigente</span>')}
                        </div>
                    </article>
                `).join('')}
            </div>
        `;

        spotlight.querySelectorAll('[data-marcar-lida-home]').forEach((botao) => {
            botao.addEventListener('click', () => marcarLidaNoInicio(Number(botao.dataset.marcarLidaHome), botao));
        });
    }

    async function marcarLidaNoInicio(id, botao) {
        await WayPoint.executar(async () => {
            if (botao) {
                botao.disabled = true;
                botao.textContent = 'Salvando...';
            }
            await WayPoint.request(`/api/notificacoes/${id}/lida`, { method: 'PATCH' });
            await carregarNotificacoes(favoritosCache);
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            WayPoint.mostrarMensagem('Notificação marcada como lida.', 'ok');
        });
    }

    function textoFavoritas(total) {
        if (!total) {
            return 'Você ainda não favoritou nenhuma linha. Favorite em Linhas para receber alertas específicos.';
        }
        return total === 1 ? 'Você acompanha 1 linha favorita.' : `Você acompanha ${total} linhas favoritas.`;
    }

    async function carregarReportes() {
        if (!resumoReportes) {
            return;
        }

        try {
            const reportes = await WayPoint.get('/api/reportes/feed');
            const lista = Array.isArray(reportes) ? reportes : [];
            const recentes = lista
                    .slice()
                    .sort((a, b) => new Date(b.dataHora) - new Date(a.dataHora))
                    .slice(0, 4);

            if (kpiReportesValor) {
                kpiReportesValor.textContent = String(lista.length);
            }
            if (kpiReportesDetalhe) {
                kpiReportesDetalhe.textContent = lista.length === 0
                        ? 'Sem relatos nos últimos 20 min'
                        : (lista.length === 1 ? '1 relato ativo agora' : `${lista.length} relatos ativos agora`);
            }

            if (!recentes.length) {
                resumoReportes.innerHTML = `<div class="empty">Nenhum reporte recente.${autenticado ? ' Em Paradas e mapa, escolha uma parada para informar a situação.' : ' Quando passageiros enviarem relatos, eles aparecerão aqui.'}</div>`;
                return;
            }

            resumoReportes.innerHTML = `
                <ul class="mini-feed">
                    ${recentes.map((reporte) => `
                        <li data-tipo="${WayPoint.escapeHtml(reporte.tipoReporte)}">
                            <div>
                                <strong>${WayPoint.escapeHtml(TEXTO_REPORTE[reporte.tipoReporte] || reporte.tipoReporte)}</strong>
                                <span>${WayPoint.escapeHtml(reporte.linhaCodigo)} - ${WayPoint.escapeHtml(reporte.linhaNome)}${reporte.paradaNome ? `, parada ${WayPoint.escapeHtml(reporte.paradaNome)}` : ''}</span>
                                <time datetime="${WayPoint.escapeHtml(reporte.dataHora)}" title="${WayPoint.escapeHtml(WayPoint.formatarData(reporte.dataHora))}">${tempoRelativo(reporte.dataHora)} • ${reporte.quantidadeConfirmacoes ?? 0} conf.</time>
                            </div>
                        </li>
                    `).join('')}
                </ul>
            `;
        } catch (erro) {
            if (kpiReportesValor) {
                kpiReportesValor.textContent = '--';
            }
            resumoReportes.innerHTML = `<div class="empty">Não foi possível carregar os reportes. ${WayPoint.escapeHtml(erro.message || '')}</div>`;
        }
    }

    function tempoRelativo(valor) {
        const data = new Date(valor);
        if (Number.isNaN(data.getTime())) {
            return '';
        }

        const minutos = Math.floor(Math.max(0, Date.now() - data.getTime()) / 60000);
        if (minutos < 1) {
            return 'agora';
        }
        if (minutos < 60) {
            return `há ${minutos} min`;
        }

        const horas = Math.floor(minutos / 60);
        return horas < 24 ? `há ${horas} h` : `há ${Math.floor(horas / 24)} d`;
    }
})();
