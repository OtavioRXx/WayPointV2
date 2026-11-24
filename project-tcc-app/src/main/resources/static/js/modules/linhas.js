(function () {
    const autenticado = document.body.dataset.authenticated === 'true';
    const usuarioId = Number(document.body.dataset.userId);
    const podeFavoritar = autenticado && usuarioId > 0;
    const lista = document.getElementById('linhasLista');
    let linhas = [];
    let favoritos = new Set();

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById('btnAtualizarLinhas')?.addEventListener('click', carregar);
        carregar();
    });

    async function carregar() {
        await WayPoint.executar(async () => {
            WayPoint.mostrarCarregando(lista, 'Carregando linhas');
            linhas = await WayPoint.get('/api/linhas');
            if (podeFavoritar) {
                const favoritosResposta = await WayPoint.get(`/api/usuarios/${usuarioId}/linhas-favoritas`);
                favoritos = new Set(favoritosResposta.map((favorito) => Number(favorito.linhaId)));
            } else {
                favoritos = new Set();
            }
            render();
        });
    }

    function render() {
        if (!linhas.length) {
            lista.innerHTML = '<div class="empty">Nenhuma linha encontrada.</div>';
            return;
        }

        lista.innerHTML = `<div class="line-grid">${linhas.map((linha) => {
            const favoritada = favoritos.has(Number(linha.id));
            return `
                <article class="line-card">
                    <div class="line-card-main">
                        <span class="line-icon" title="Linha ${WayPoint.escapeHtml(linha.codigo)}">${WayPoint.escapeHtml(linha.codigo)}</span>
                        <div>
                        <strong class="line-title">${WayPoint.escapeHtml(linha.nome)}</strong>
                        <span class="line-detail">Consulte a rota e acompanhe informações atualizadas desta linha.</span>
                        <div class="status-line">
                            <span>Status:</span>
                            <span class="badge status-badge ${classeStatusLinha(linha.status)}">${WayPoint.escapeHtml(textoStatusLinha(linha.status))}</span>
                        </div>
                        </div>
                    </div>
                    <div class="line-actions">
                        <a class="button ghost" href="/paradas?linhaId=${encodeURIComponent(linha.id)}">Ver rota no mapa</a>
                        ${podeFavoritar
                                ? `<button class="favorite-button ${favoritada ? 'favorited' : 'success'}" type="button" data-linha-id="${linha.id}" data-favoritada="${favoritada}">${favoritada ? 'Desfavoritar' : 'Favoritar'}</button>`
                                : `<a class="button ghost" href="/login" title="Entre na sua conta para favoritar esta linha">Entrar para favoritar</a>`}
                    </div>
                </article>
            `;
        }).join('')}</div>`;

        if (podeFavoritar) {
            lista.querySelectorAll('button[data-linha-id]').forEach((botao) => {
                botao.addEventListener('click', () => alternarFavorito(Number(botao.dataset.linhaId), botao.dataset.favoritada === 'true'));
            });
        }
    }

    async function alternarFavorito(linhaId, favoritada) {
        if (!podeFavoritar) {
            WayPoint.mostrarMensagem('Entre na sua conta para favoritar linhas.', 'error');
            return;
        }

        await WayPoint.executar(async () => {
            if (favoritada) {
                await WayPoint.request(`/api/usuarios/${usuarioId}/linhas-favoritas/${linhaId}`, { method: 'DELETE' });
                favoritos.delete(linhaId);
                WayPoint.mostrarMensagem('Linha removida dos favoritos.', 'ok');
            } else {
                await WayPoint.request(`/api/usuarios/${usuarioId}/linhas-favoritas/${linhaId}`, { method: 'POST' });
                favoritos.add(linhaId);
                WayPoint.mostrarMensagem('Linha adicionada aos favoritos.', 'ok');
            }
            render();
            lista.querySelector(`[data-linha-id="${linhaId}"]`)?.classList.add('just-changed');
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
        });
    }

    function textoStatusLinha(status) {
        const textos = {
            NORMAL: 'Normal',
            ATRASADA: 'Atrasada',
            LOTADA: 'Lotada',
            INTERROMPIDA: 'Interrompida',
            UNIFICADA: 'Unificada'
        };

        return textos[status] || status || 'Normal';
    }

    function classeStatusLinha(status) {
        return `status-${String(status || 'NORMAL').toLowerCase()}`;
    }
})();
