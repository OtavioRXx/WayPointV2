(function () {
    async function request(url, options = {}) {
        let response;
        let data = null;
        const method = (options.method || 'GET').toUpperCase();

        try {
            response = await fetch(url, {
                credentials: 'same-origin',
                ...options,
                headers: {
                    Accept: 'application/json',
                    ...csrfHeaders(method),
                    ...(options.headers || {})
                }
            });
        } catch (erro) {
            console.error('Falha de rede ao chamar API', { url, erro });
            throw new Error('Não foi possível conectar ao servidor.');
        }

        if (response.redirected && response.url && response.url.includes('/login')) {
            throw new Error('Esta ação exige login. Entre na sua conta para continuar.');
        }

        if (response.status !== 204) {
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('text/html') && !response.ok) {
                throw new Error(mensagemErro('', response.status));
            }
            data = contentType.includes('application/json') ? await response.json() : await response.text();
        }

        if (!response.ok) {
            throw new Error(mensagemErro(data, response.status));
        }

        return data;
    }

    function get(url) {
        return request(url);
    }

    function json(url, method, body) {
        return request(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
    }

    function csrfHeaders(method) {
        if (['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
            return {};
        }

        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        return token && header ? { [header]: token } : {};
    }

    async function executar(acao) {
        try {
            limparMensagem();
            return await acao();
        } catch (erro) {
            mostrarMensagem(erro.message || 'Não foi possível concluir a ação.', 'error');
            return null;
        }
    }

    function popularSelectLinhas(select, linhas, permitirVazio = false) {
        if (!select) {
            return;
        }

        const inicio = permitirVazio ? '<option value="">Todas as linhas</option>' : '<option value="">Selecione uma linha</option>';
        select.innerHTML = inicio + linhas.map((linha) =>
            `<option value="${linha.id}">${escapeHtml(linha.codigo)} - ${escapeHtml(linha.nome)}</option>`
        ).join('');
    }

    function mostrarMensagem(texto, tipo = 'ok') {
        const elemento = document.getElementById('mensagem');
        if (!elemento) {
            return;
        }

        elemento.textContent = texto;
        elemento.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
        elemento.setAttribute('aria-live', tipo === 'error' ? 'assertive' : 'polite');
        elemento.className = `alert ${tipo}`;

        window.clearTimeout(elemento.dataset.timerId);
        elemento.dataset.timerId = window.setTimeout(() => {
            limparMensagem();
        }, tipo === 'error' ? 6500 : 4200);
    }

    function limparMensagem() {
        const elemento = document.getElementById('mensagem');
        if (!elemento) {
            return;
        }

        elemento.textContent = '';
        elemento.className = 'alert';
    }

    function mensagemErro(data, status) {
        if (data && typeof data === 'object') {
            return data.mensagem || data.message || data.erro || data.error || JSON.stringify(data);
        }

        const texto = String(data || '').trim();
        if (texto && !texto.toLowerCase().includes('<html')) {
            return texto;
        }

        if (status === 403) {
            return 'Você não possui permissão para executar esta operação.';
        }

        if (status === 401 || status === 302) {
            return 'Sua sessao expirou. Entre novamente no sistema.';
        }

        return `Erro HTTP ${status}.`;
    }

    function escapeHtml(valor) {
        return String(valor ?? '')
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#039;');
    }

    function formatarData(valor) {
        return valor ? new Date(valor).toLocaleString('pt-BR') : '';
    }

    function formatarHora(valor) {
        return valor ? String(valor).slice(0, 5) : '';
    }

    function formatarConfiabilidade(valor) {
        return Number(valor ?? 0).toLocaleString('pt-BR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function mostrarCarregando(elemento, texto = 'Carregando informações...') {
        if (!elemento) {
            return;
        }

        elemento.innerHTML = `<div class="loading-block" aria-busy="true" aria-label="${escapeHtml(texto)}"></div>`;
    }

    function isAuthenticated() {
        return document.body.dataset.authenticated === 'true' && Number(document.body.dataset.userId) > 0;
    }

    window.WayPoint = {
        request,
        get,
        json,
        executar,
        popularSelectLinhas,
        mostrarMensagem,
        limparMensagem,
        escapeHtml,
        formatarData,
        formatarHora,
        formatarConfiabilidade,
        mostrarCarregando,
        isAuthenticated
    };
})();
