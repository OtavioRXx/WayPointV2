/*
 * WayPoint — comportamento da interface (somente apresentação).
 * - Abre/fecha o menu lateral em gaveta (tablet em retrato e celular).
 * - Botão "Mostrar/Ocultar" senha nos formulários de autenticação.
 * Não faz requisições e não altera dados enviados ao servidor.
 */
(function () {
    const raiz = document.documentElement;
    const CLASSE_ABERTO = 'nav-open';
    const telaLarga = window.matchMedia('(min-width: 900px)');

    function menuAberto() {
        return raiz.classList.contains(CLASSE_ABERTO);
    }

    function definirMenu(abrir) {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) {
            return;
        }

        raiz.classList.toggle(CLASSE_ABERTO, abrir);
        document.querySelectorAll('[data-nav-toggle]').forEach((botao) => {
            botao.setAttribute('aria-expanded', String(abrir));
        });

        if (abrir) {
            sidebar.querySelector('[data-nav-close]')?.focus();
        }
    }

    document.addEventListener('click', (evento) => {
        if (evento.target.closest('[data-nav-toggle]')) {
            definirMenu(!menuAberto());
            return;
        }

        if (evento.target.closest('[data-nav-close]')) {
            definirMenu(false);
            document.querySelector('[data-nav-toggle]')?.focus();
            return;
        }

        if (menuAberto() && !evento.target.closest('#sidebar')) {
            definirMenu(false);
        }
    });

    document.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape' && menuAberto()) {
            definirMenu(false);
            document.querySelector('[data-nav-toggle]')?.focus();
        }
    });

    const aoMudarLargura = (evento) => {
        if (evento.matches) {
            definirMenu(false);
        }
    };
    if (typeof telaLarga.addEventListener === 'function') {
        telaLarga.addEventListener('change', aoMudarLargura);
    } else if (typeof telaLarga.addListener === 'function') {
        telaLarga.addListener(aoMudarLargura);
    }

    document.querySelectorAll('[data-password-toggle]').forEach((botao) => {
        const campo = document.getElementById(botao.dataset.passwordToggle);
        if (!campo) {
            botao.hidden = true;
            return;
        }

        botao.addEventListener('click', () => {
            const mostrar = campo.type === 'password';
            campo.type = mostrar ? 'text' : 'password';
            botao.textContent = mostrar ? 'Ocultar' : 'Mostrar';
            botao.setAttribute('aria-pressed', String(mostrar));
            botao.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
            campo.focus();
        });
    });

    function formatarCpf(valor) {
        const digitos = String(valor || '').replace(/\D/g, '').slice(0, 11);
        if (digitos.length <= 3) {
            return digitos;
        }
        if (digitos.length <= 6) {
            return `${digitos.slice(0, 3)}.${digitos.slice(3)}`;
        }
        if (digitos.length <= 9) {
            return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6)}`;
        }
        return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`;
    }

    function posicaoAposDigito(textoFormatado, quantidadeDigitos) {
        if (quantidadeDigitos <= 0) {
            return 0;
        }
        let encontrados = 0;
        for (let i = 0; i < textoFormatado.length; i++) {
            if (/\d/.test(textoFormatado[i])) {
                encontrados++;
                if (encontrados === quantidadeDigitos) {
                    return i + 1;
                }
            }
        }
        return textoFormatado.length;
    }

    function aplicarMascaraCpf(campo) {
        const valorAnterior = campo.value;
        const selecaoAnterior = typeof campo.selectionStart === 'number' ? campo.selectionStart : valorAnterior.length;
        const digitosAntesCursor = valorAnterior.slice(0, selecaoAnterior).replace(/\D/g, '').length;
        const formatado = formatarCpf(valorAnterior);

        if (formatado !== valorAnterior) {
            campo.value = formatado;
            if (document.activeElement === campo && typeof campo.setSelectionRange === 'function') {
                const novaPosicao = posicaoAposDigito(formatado, digitosAntesCursor);
                campo.setSelectionRange(novaPosicao, novaPosicao);
            }
        }
    }

    document.querySelectorAll('input[data-cpf-mask]').forEach((campo) => {
        if (campo.value) {
            campo.value = formatarCpf(campo.value);
        }

        campo.addEventListener('input', () => aplicarMascaraCpf(campo));
        campo.addEventListener('paste', () => {
            window.setTimeout(() => aplicarMascaraCpf(campo), 0);
        });
        campo.addEventListener('blur', () => {
            campo.value = formatarCpf(campo.value);
        });
    });
})();
