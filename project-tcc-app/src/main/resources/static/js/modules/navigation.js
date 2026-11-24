(function () {
    const INTERVALO_SINCRONIZACAO_MS = 45000;
    let sincronizando = false;

    document.addEventListener('DOMContentLoaded', () => {
        atualizarIndicadorNotificacoes();

        window.addEventListener('waypoint:notificacoes-atualizadas', atualizarIndicadorNotificacoes);
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible') {
                atualizarIndicadorNotificacoes();
            }
        });

        window.setInterval(() => {
            if (document.visibilityState === 'visible') {
                atualizarIndicadorNotificacoes();
            }
        }, INTERVALO_SINCRONIZACAO_MS);
    });

    async function atualizarIndicadorNotificacoes() {
        const links = document.querySelectorAll('[data-notification-nav]');
        if (!links.length || sincronizando) {
            return;
        }

        const autenticado = document.body.dataset.authenticated === 'true';
        const usuarioId = Number(document.body.dataset.userId);
        const url = (autenticado && usuarioId)
                ? `/api/usuarios/${usuarioId}/notificacoes?status=ATIVA&lida=false`
                : '/api/notificacoes/publicas?status=ATIVA';

        sincronizando = true;
        try {
            const resposta = await fetch(url, {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            });

            if (!resposta.ok) {
                return;
            }

            const contentType = resposta.headers.get('content-type') || '';
            if (!contentType.includes('application/json')) {
                return;
            }

            const notificacoes = await resposta.json();
            const total = Array.isArray(notificacoes) ? notificacoes.length : 0;

            links.forEach((link) => {
                link.classList.toggle('has-unread', total > 0);
                const badge = link.querySelector('[data-notification-badge]');
                if (badge) {
                    if (total > 0) {
                        badge.textContent = total > 99 ? '99+' : String(total);
                        badge.classList.remove('hidden');
                        badge.setAttribute('aria-label', autenticado
                                ? `${total} notificações não lidas`
                                : `${total} comunicados oficiais ativos`);
                    } else {
                        badge.textContent = '';
                        badge.classList.add('hidden');
                    }
                }
            });
        } catch (erro) {
            console.debug('Não foi possível atualizar o indicador de notificações.', erro);
        } finally {
            sincronizando = false;
        }
    }

    window.WayPointNav = {
        atualizarIndicadorNotificacoes
    };
})();
