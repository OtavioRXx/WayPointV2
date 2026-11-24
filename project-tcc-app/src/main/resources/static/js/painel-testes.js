const painelDataset = document.body.dataset;

    const usuarioLogado = {
        id: painelDataset.userId ? Number(painelDataset.userId) : null,
        nome: painelDataset.userName || '',
        email: painelDataset.userEmail || '',
        tipo: painelDataset.userType || 'USER',
        admin: painelDataset.userAdmin === 'true'
    };

    const googleMapsConfig = {
        apiKey: painelDataset.googleMapsApiKey || '',
        mapId: painelDataset.googleMapsMapId || 'DEMO_MAP_ID'
    };

    const estado = {
        linhas: [],
        usuarios: [],
        linhaSelecionadaId: '',
        sentidoParadas: 'IDA',
        mapaParadas: null,
        mapaParadasMarkers: [],
        mapaParadasPolylines: [],
        mapaParadasInfoWindow: null,
        mapaParadasElemento: null,
        programacaoTimer: null,
        googleMapsPromise: null,
        googleMapsScriptId: 'waypoint-google-maps-js',
        rotaRequisicaoAtual: 0
    };

    document.addEventListener('DOMContentLoaded', inicializar);

    async function inicializar() {
        configurarAbas();
        configurarFormularios();
        configurarSeletoresDeLinha();
        document.getElementById('btnAtualizarLinhas').addEventListener('click', () => carregarLinhas());

        await executarAcao(async () => {
            await carregarLinhas(false);
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
        });
    }

    function configurarAbas() {
        document.querySelectorAll('.tab').forEach((tab) => {
            tab.addEventListener('click', () => abrirAba(tab.dataset.view));
        });
    }

    function configurarFormularios() {
        document.getElementById('formLinha')?.addEventListener('submit', salvarLinha);
        document.getElementById('formParada')?.addEventListener('submit', salvarParada);
        document.getElementById('formOcorrencia')?.addEventListener('submit', salvarOcorrencia);
        document.getElementById('formReporte')?.addEventListener('submit', salvarReporte);
        document.getElementById('formNotificacao')?.addEventListener('submit', salvarNotificacao);
        document.getElementById('formAdmin')?.addEventListener('submit', salvarAdmin);
        document.querySelectorAll('input[name="destinatarios"]').forEach((radio) => {
            radio.addEventListener('change', atualizarDestinoNotificacao);
        });
        atualizarDestinoNotificacao();
    }

    function configurarSeletoresDeLinha() {
        document.addEventListener('change', (event) => {
            if (event.target.matches('[data-linha-select]')) {
                selecionarLinha(event.target.value);
            }
        });
    }

    function abrirAba(nome) {
        document.querySelectorAll('.tab').forEach((tab) => {
            tab.classList.toggle('active', tab.dataset.view === nome);
        });

        document.querySelectorAll('.view').forEach((view) => {
            view.classList.toggle('active', view.id === `view-${nome}`);
        });

        if (nome === 'paradas') {
            if (usuarioLogado.admin) {
                carregarParadas();
            } else {
                prepararMapaParadasUsuario();
            }
        }

        if (nome === 'ocorrencias') {
            carregarOcorrencias();
        }

        if (nome === 'reportes') {
            carregarFeedReportes();
        }

        if (nome === 'favoritos' && !usuarioLogado.admin) {
            carregarFavoritos();
        }

        if (nome === 'notificacoes') {
            carregarNotificacoes();
        }

        if (nome === 'usuarios' && usuarioLogado.admin) {
            carregarUsuarios(false);
        }
    }

    async function carregarLinhas(mostrarSucesso = true) {
        await executarAcao(async () => {
            estado.linhas = await apiGet('/api/linhas');
            renderLinhas();
            popularSelectsDeLinha();
            if (!usuarioLogado.admin && document.getElementById('view-paradas')?.classList.contains('active')) {
                await prepararMapaParadasUsuario();
            }
            if (mostrarSucesso) {
                mostrarMensagem('Linhas carregadas com sucesso.', 'ok');
            }
        });
    }

    async function carregarUsuarios(mostrarSucesso = true) {
        await executarAcao(async () => {
            mostrarCarregandoResultado('usuariosResultado', 'Carregando usuários...');
            estado.usuarios = await apiGet('/api/usuarios');
            popularSelectsDeUsuario();
            renderUsuarios();
            if (mostrarSucesso) {
                mostrarMensagem('Usuários carregados com sucesso.', 'ok');
            }
        });
    }

    function mostrarCarregandoResultado(elementId, texto) {
        const elemento = document.getElementById(elementId);
        if (elemento) {
            elemento.innerHTML = `<div class="loading-block" aria-busy="true" aria-label="${escapeHtml(texto)}"></div>`;
        }
    }

    async function carregarResumoAdmin() {
        const ids = {
            linhas: document.getElementById('adminTotalLinhas'),
            paradas: document.getElementById('adminTotalParadas'),
            ocorrencias: document.getElementById('adminTotalOcorrencias'),
            reportes: document.getElementById('adminTotalReportes'),
            notificacoes: document.getElementById('adminTotalNotificacoes'),
            usuarios: document.getElementById('adminTotalUsuarios')
        };

        if (!ids.linhas) {
            return;
        }

        const resultados = await Promise.allSettled([
            Promise.resolve(estado.linhas),
            apiGet('/api/paradas'),
            apiGet('/api/ocorrencias?ativa=true'),
            apiGet('/api/reportes/feed'),
            apiGet('/api/notificacoes/avisos'),
            apiGet('/api/usuarios')
        ]);

        atualizarResumo(ids.linhas, resultados[0]);
        atualizarResumo(ids.paradas, resultados[1]);
        atualizarResumo(ids.ocorrencias, resultados[2]);
        atualizarResumo(ids.reportes, resultados[3]);
        atualizarResumo(ids.notificacoes, resultados[4]);
        atualizarResumo(ids.usuarios, resultados[5]);
    }

    function atualizarResumo(elemento, resultado) {
        if (!elemento) {
            return;
        }

        if (resultado.status !== 'fulfilled' || !Array.isArray(resultado.value)) {
            elemento.textContent = 'Indisp.';
            return;
        }

        elemento.textContent = String(resultado.value.length);
    }

    async function carregarParadas() {
        await executarAcao(async () => {
            mostrarCarregandoResultado('paradasResultado', 'Carregando paradas...');
            renderParadas(await apiGet('/api/paradas'));
        });
    }

    async function carregarParadasDaLinha() {
        if (!usuarioLogado.admin) {
            await carregarMapaParadasUsuario();
            return;
        }

        const linhaId = valorLinhaSelecionada('paradasConsultaLinha');
        if (!linhaId) {
            mostrarMensagem('Selecione uma linha para consultar as paradas.', 'error');
            return;
        }

        await executarAcao(async () => {
            const sentido = document.getElementById('filtroSentido')?.value || '';
            const query = sentido ? `?sentido=${encodeURIComponent(sentido)}` : '';
            renderParadas(await apiGet(`/api/linhas/${linhaId}/paradas${query}`));
        });
    }

    async function prepararMapaParadasUsuario() {
        if (usuarioLogado.admin || estado.linhas.length === 0) {
            return;
        }

        if (!estado.linhaSelecionadaId) {
            selecionarLinha(String(estado.linhas[0].id), false);
        }

        await carregarMapaParadasUsuario();
    }

    async function carregarMapaParadasUsuario() {
        const linhaId = valorLinhaSelecionada('paradasConsultaLinha');
        if (!linhaId) {
            mostrarMensagem('Selecione uma linha para visualizar a rota.', 'error');
            return;
        }

        await executarAcao(async () => {
            const requisicao = ++estado.rotaRequisicaoAtual;
            atualizarBotoesSentidoParadas();
            const paradas = await apiGet(`/api/linhas/${linhaId}/paradas?sentido=${estado.sentidoParadas}`);
            if (requisicao !== estado.rotaRequisicaoAtual) {
                return;
            }
            await renderMapaParadas(paradas, requisicao);
        });
    }

    function selecionarSentidoParadas(sentido) {
        estado.sentidoParadas = sentido;
        atualizarBotoesSentidoParadas();
        carregarMapaParadasUsuario();
    }

    async function carregarOcorrencias() {
        await executarAcao(async () => {
            mostrarCarregandoResultado('ocorrenciasResultado', 'Carregando ocorrências...');
            const endpoint = usuarioLogado.admin ? '/api/ocorrencias' : '/api/ocorrencias?ativa=true';
            renderOcorrencias(await apiGet(endpoint));
        });
    }

    async function carregarOcorrenciasDaLinha() {
        const linhaId = valorLinhaSelecionada('ocorrenciasConsultaLinha');
        if (!linhaId) {
            mostrarMensagem('Selecione uma linha para consultar ocorrências.', 'error');
            return;
        }

        await executarAcao(async () => {
            mostrarCarregandoResultado('ocorrenciasResultado', 'Carregando ocorrências da linha...');
            const query = usuarioLogado.admin ? '' : '?ativa=true';
            renderOcorrencias(await apiGet(`/api/linhas/${linhaId}/ocorrencias${query}`));
        });
    }

    async function carregarFeedReportes() {
        await executarAcao(async () => {
            mostrarCarregandoResultado('reportesResultado', 'Carregando feed de reportes...');
            renderReportes(await apiGet('/api/reportes/feed'));
        });
    }

    async function carregarReportesDaLinha() {
        const linhaId = valorLinhaSelecionada('reportesLinhaSelect');
        if (!linhaId) {
            mostrarMensagem('Selecione uma linha para consultar reportes.', 'error');
            return;
        }

        await executarAcao(async () => {
            mostrarCarregandoResultado('reportesResultado', 'Carregando reportes da linha...');
            renderReportes(await apiGet(`/api/reportes/feed?linhaId=${linhaId}`));
        });
    }

    async function carregarFavoritos() {
        await executarAcao(async () => {
            renderFavoritos(await apiGet(`/api/usuarios/${usuarioLogado.id}/linhas-favoritas`));
        });
    }

    async function carregarNotificacoes() {
        await executarAcao(async () => {
            mostrarCarregandoResultado('notificacoesResultado', 'Carregando notificações...');
            if (usuarioLogado.admin) {
                renderAvisosAdmin(await apiGet('/api/notificacoes/avisos'));
                return;
            }

            const lida = document.getElementById('filtroLida')?.value || '';
            const filtroLida = lida ? `&lida=${encodeURIComponent(lida)}` : '';
            const atuais = await apiGet(`/api/usuarios/${usuarioLogado.id}/notificacoes?status=ATIVA${filtroLida}`);
            const historico = await apiGet(`/api/usuarios/${usuarioLogado.id}/notificacoes?status=SUPERADA${filtroLida}`);
            renderNotificacoesUsuario(atuais, historico);
        });
    }

    async function salvarLinha(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            await apiJson('/api/linhas', 'POST', {
                codigo: form.codigo.value,
                nome: form.nome.value,
                status: form.status.value
            });
            form.reset();
            await carregarLinhas(false);
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            mostrarMensagem('Linha cadastrada com sucesso.', 'ok');
        });
    }

    async function salvarParada(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            await apiJson('/api/paradas', 'POST', {
                linhaId: Number(form.linhaId.value),
                nome: form.nome.value,
                latitude: Number(form.latitude.value),
                longitude: Number(form.longitude.value),
                ordem: Number(form.ordem.value),
                sentido: form.sentido.value
            });
            form.reset();
            popularSelectsDeLinha();
            if (usuarioLogado.admin) {
                await carregarParadas();
                await carregarResumoAdmin();
            }
            mostrarMensagem('Parada cadastrada com sucesso.', 'ok');
        });
    }

    async function salvarOcorrencia(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            await apiJson('/api/ocorrencias', 'POST', {
                linhaId: Number(form.linhaId.value),
                tipo: form.tipo.value,
                descricao: form.descricao.value,
                prioridade: form.prioridade.value,
                ativa: form.ativa.value === 'true'
            });
            form.reset();
            popularSelectsDeLinha();
            await carregarOcorrencias();
            await carregarLinhas(false);
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem('Ocorrencia criada com sucesso. O status da linha foi recalculado.', 'ok');
        });
    }

    async function salvarReporte(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            await apiJson('/api/reportes', 'POST', {
                usuarioId: usuarioLogado.id,
                linhaId: Number(form.linhaId.value),
                tipoReporte: form.tipoReporte.value
            });
            await carregarFeedReportes();
            mostrarMensagem('Reporte enviado com sucesso.', 'ok');
        });
    }

    async function salvarNotificacao(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            const enviarParaTodos = form.destinatarios.value === 'TODOS';
            const linhaId = enviarParaTodos ? null : Number(form.linhaId.value);
            if (!enviarParaTodos && !linhaId) {
                throw new Error('Selecione uma linha para enviar aviso a usuários de uma linha específica.');
            }

            const resposta = await apiJson('/api/notificacoes', 'POST', {
                linhaId,
                enviarParaTodos,
                titulo: form.titulo.value,
                mensagem: form.mensagem.value,
                superarNotificacoesAtuaisDaLinha: !enviarParaTodos && form.superarNotificacoesAtuaisDaLinha.checked
            });
            form.reset();
            popularSelectsDeLinha();
            atualizarDestinoNotificacao();
            await carregarNotificacoes();
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem(`${resposta.usuariosNotificados} usuário(s) notificado(s). ${resposta.mensagem}`, 'ok');
        });
    }

    async function salvarAdmin(event) {
        event.preventDefault();
        const form = event.currentTarget;
        await executarAcao(async () => {
            const resposta = await apiJson('/api/usuarios/admins', 'POST', {
                nome: form.nome.value,
                cpf: form.cpf.value,
                dataNascimento: form.dataNascimento.value,
                email: form.email.value,
                senha: form.senha.value
            });
            form.reset();
            await carregarUsuarios(false);
            await carregarResumoAdmin();
            mostrarMensagem(`Administrador "${resposta.nome}" (${resposta.email}) cadastrado com sucesso.`, 'ok');
        });
    }

    function atualizarDestinoNotificacao() {
        const form = document.getElementById('formNotificacao');
        const campoLinha = document.getElementById('campoLinhaNotificacao');
        const selectLinha = document.getElementById('notificacaoLinhaId');
        const checkboxSuperar = document.getElementById('notificacaoSuperar');
        if (!form || !campoLinha || !selectLinha) {
            return;
        }

        const avisoPorLinha = form.destinatarios.value === 'LINHA';
        campoLinha.classList.toggle('hidden', !avisoPorLinha);
        selectLinha.required = avisoPorLinha;
        if (checkboxSuperar) {
            checkboxSuperar.disabled = !avisoPorLinha;
            if (!avisoPorLinha) {
                checkboxSuperar.checked = false;
            }
        }
    }

    async function confirmarReporte(reporteId) {
        await executarAcao(async () => {
            definirBotaoConfirmacao(reporteId, true);
            try {
                const resposta = await apiRequest(`/api/reportes/${reporteId}/confirmacoes`, { method: 'POST' });
                await carregarFeedReportes();
                mostrarMensagem(`${resposta.mensagem} Confirmações: ${resposta.quantidadeConfirmacoes}.`, 'ok');
            } catch (erro) {
                definirBotaoConfirmacao(reporteId, false);
                throw erro;
            }
        });
    }

    async function favoritarLinha(linhaId) {
        await executarAcao(async () => {
            await apiRequest(`/api/usuarios/${usuarioLogado.id}/linhas-favoritas/${linhaId}`, { method: 'POST' });
            mostrarMensagem('Linha favoritada.', 'ok');
        });
    }

    async function desfavoritarLinha(linhaId) {
        await executarAcao(async () => {
            await apiRequest(`/api/usuarios/${usuarioLogado.id}/linhas-favoritas/${linhaId}`, { method: 'DELETE' });
            await carregarFavoritos();
            mostrarMensagem('Linha removida dos favoritos.', 'ok');
        });
    }

    async function encerrarOcorrencia(ocorrenciaId) {
        await executarAcao(async () => {
            await apiRequest(`/api/ocorrencias/${ocorrenciaId}/encerrar`, { method: 'PATCH' });
            await carregarOcorrencias();
            await carregarLinhas(false);
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem('Ocorrencia encerrada. O status da linha foi recalculado.', 'ok');
        });
    }

    async function marcarNotificacaoLida(notificacaoId) {
        await executarAcao(async () => {
            await apiRequest(`/api/notificacoes/${notificacaoId}/lida`, { method: 'PATCH' });
            await carregarNotificacoes();
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem('Notificação marcada como lida.', 'ok');
        });
    }

    async function marcarAvisoSuperado(avisoId, referenciaNotificacaoId) {
        await executarAcao(async () => {
            const endpoint = avisoId
                    ? `/api/notificacoes/avisos/oficial/${avisoId}/superada`
                    : `/api/notificacoes/avisos/${referenciaNotificacaoId}/superada`;
            const resposta = await apiRequest(endpoint, { method: 'PATCH' });
            await carregarNotificacoes();
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem(`Aviso "${resposta.titulo}" marcado como SUPERADA para ${resposta.usuariosAlcancados} usuário(s).`, 'ok');
        });
    }

    async function excluirAviso(avisoId, titulo) {
        const nomeAviso = titulo || 'este aviso';
        if (!confirm(`Deseja realmente excluir definitivamente "${nomeAviso}"?\n\nEssa ação removerá a notificação do banco de dados, do histórico e dos indicadores de todos os usuários.`)) {
            return;
        }

        await executarAcao(async () => {
            await apiRequest(`/api/notificacoes/avisos/${avisoId}`, { method: 'DELETE' });
            await carregarNotificacoes();
            if (usuarioLogado.admin) {
                await carregarResumoAdmin();
            }
            window.dispatchEvent(new CustomEvent('waypoint:notificacoes-atualizadas'));
            mostrarMensagem(`Aviso "${nomeAviso}" excluído com sucesso.`, 'ok');
        });
    }

    async function limparHistoricoNotificacoes() {
        if (!confirm('Deseja realmente limpar seu histórico de notificações?')) {
            return;
        }

        await executarAcao(async () => {
            const resposta = await apiRequest('/api/notificacoes/minhas/historico', { method: 'DELETE' });
            await carregarNotificacoes();
            mostrarMensagem(`${resposta.mensagem} Removidas: ${resposta.notificacoesRemovidas}.`, 'ok');
        });
    }

    function selecionarLinha(linhaId, mostrarSelecao = true) {
        estado.linhaSelecionadaId = linhaId || '';
        document.querySelectorAll('[data-linha-select]').forEach((select) => {
            if ([...select.options].some((option) => option.value === estado.linhaSelecionadaId)) {
                select.value = estado.linhaSelecionadaId;
            }
        });

        if (linhaId && mostrarSelecao) {
            const linha = estado.linhas.find((item) => String(item.id) === String(linhaId));
            if (linha) {
                mostrarMensagem(`Linha selecionada: ${linha.codigo} - ${linha.nome}.`, 'ok');
            }
        }
    }

    function valorLinhaSelecionada(id) {
        return document.getElementById(id)?.value || estado.linhaSelecionadaId;
    }

    function popularSelectsDeLinha() {
        const options = estado.linhas.map((linha) =>
            `<option value="${linha.id}">${escapeHtml(linha.codigo)} - ${escapeHtml(linha.nome)} (${escapeHtml(linha.status || 'NORMAL')})</option>`
        ).join('');

        document.querySelectorAll('[data-linha-select]').forEach((select) => {
            const allowEmpty = select.dataset.allowEmpty === 'true';
            const label = allowEmpty ? 'Sem linha vinculada' : 'Selecione uma linha';
            select.innerHTML = `<option value="">${label}</option>${options}`;
            if (estado.linhaSelecionadaId && [...select.options].some((option) => option.value === estado.linhaSelecionadaId)) {
                select.value = estado.linhaSelecionadaId;
            }
        });
    }

    function popularSelectsDeUsuario() {
        const options = estado.usuarios.map((usuario) =>
            `<option value="${usuario.id}">${escapeHtml(usuario.nome)} - ${escapeHtml(usuario.email)} (${escapeHtml(usuario.tipoUsuario)})</option>`
        ).join('');

        document.querySelectorAll('[data-usuario-select]').forEach((select) => {
            select.innerHTML = `<option value="">Selecione um usuário</option>${options}`;
        });
    }

    function renderLinhas() {
        const podeFavoritar = !usuarioLogado.admin;
        const html = tabelaOuVazio(estado.linhas, `
            <table>
                <thead>
                    <tr>
                        <th>Código</th>
                        <th>Nome</th>
                        <th>Status</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${estado.linhas.map((linha) => `
                        <tr>
                            <td>${escapeHtml(linha.codigo)}</td>
                            <td>${escapeHtml(linha.nome)}</td>
                            <td><span class="badge status-badge status-${escapeHtml(String(linha.status || 'NORMAL').toLowerCase())}">${escapeHtml(textoStatusLinha(linha.status))}</span></td>
                            <td>
                                <div class="row-actions">
                                    ${podeFavoritar ? `<button type="button" onclick="favoritarLinha(${linha.id})">Favoritar</button>` : ''}
                                    ${!podeFavoritar ? '<span class="status-line">Use o formulário para administrar linhas.</span>' : ''}
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhuma linha encontrada.');
        document.getElementById('linhasResultado').innerHTML = html;
    }

    function renderParadas(paradas) {
        const html = tabelaOuVazio(paradas, `
            <table>
                <thead>
                    <tr>
                        <th>Linha</th>
                        <th>Parada</th>
                        <th>Sentido</th>
                        <th>Ordem</th>
                        <th>Latitude</th>
                        <th>Longitude</th>
                    </tr>
                </thead>
                <tbody>
                    ${paradas.map((parada) => `
                        <tr>
                            <td>${escapeHtml(parada.linhaCodigo || '')} - ${escapeHtml(parada.linhaNome || '')}</td>
                            <td>${escapeHtml(parada.nome)}</td>
                            <td>${escapeHtml(parada.sentido || '')}</td>
                            <td>${parada.ordem ?? ''}</td>
                            <td>${parada.latitude ?? ''}</td>
                            <td>${parada.longitude ?? ''}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhuma parada encontrada.');
        document.getElementById('paradasResultado').innerHTML = html;
    }

    async function renderMapaParadas(paradas, requisicao) {
        const linha = estado.linhas.find((item) => String(item.id) === String(valorLinhaSelecionada('paradasConsultaLinha')));
        const tituloLinha = linha ? `${linha.codigo} - ${linha.nome}` : 'Linha selecionada';

        if (!Array.isArray(paradas) || paradas.length === 0) {
            limparMapaParadas();
            estado.mapaParadas = null;
            estado.mapaParadasElemento = null;
            document.getElementById('paradasResultado').innerHTML = `
                <div class="empty">Nenhuma parada cadastrada para ${escapeHtml(tituloLinha)} no sentido ${estado.sentidoParadas}.</div>
            `;
            return;
        }

        garantirEstruturaMapaParadas();
        atualizarResumoRota(tituloLinha, paradas);
        renderProgramacaoPainel(paradas);
        mostrarStatusMapa('Carregando rota...');

        console.info(`Google Maps API Key configurada: ${googleMapsConfig.apiKey ? 'sim' : 'não'}`);
        console.info(`Google Maps Map ID configurado: ${googleMapsConfig.mapId ? 'sim' : 'não'}`);

        if (!googleMapsConfig.apiKey) {
            mostrarStatusMapa('Google Maps não configurado. Defina GOOGLE_MAPS_API_KEY.');
            return;
        }

        try {
            await carregarGoogleMapsApi();
            await inicializarMapaParadas(paradas);
            if (requisicao !== estado.rotaRequisicaoAtual) {
                return;
            }
            const rotaPelasVias = await desenharRotaParadas(paradas);
            if (requisicao !== estado.rotaRequisicaoAtual) {
                return;
            }
            if (rotaPelasVias) {
                mostrarStatusMapa('Rota carregada respeitando a ordem cadastrada das paradas.');
            }
        } catch (erro) {
            console.error('Falha ao carregar ou desenhar Google Maps', erro);
            limparMapaParadas();
            mostrarStatusMapa(WayPointMapa.mensagemErroMaps(erro));
        }
    }

    function garantirEstruturaMapaParadas() {
        if (document.getElementById('mapaParadas')) {
            return;
        }

        document.getElementById('paradasResultado').innerHTML = `
            <div class="map-shell compact-card">
                <div id="rotaResumo" class="route-summary"></div>
                <div class="route-layout">
                    <div id="mapaParadas" class="map-canvas" role="img" aria-label="Mapa da rota selecionada"></div>
                    <aside class="card route-stops">
                        <h3>Paradas da rota</h3>
                        <ol id="listaParadasRota"></ol>
                    </aside>
                </div>
                <section id="programacaoAtualPainel" class="schedule-panel" aria-live="polite"></section>
                <div id="mapaParadasStatus" class="empty">Carregando rota...</div>
            </div>
        `;
    }

    function renderProgramacaoPainel(paradas) {
        const elemento = document.getElementById('programacaoAtualPainel');
        if (!elemento || !window.WayPointProgramacao) {
            return;
        }

        if (estado.programacaoTimer) {
            window.clearInterval(estado.programacaoTimer);
            estado.programacaoTimer = null;
        }

        const atualizar = () => {
            if (!Array.isArray(paradas) || paradas.length === 0) {
                elemento.innerHTML = '<div class="empty">Nenhuma parada cadastrada para calcular a programação desta rota.</div>';
                return;
            }

            const extremos = WayPointProgramacao.extremosDaRota(paradas, estado.sentidoParadas);
            const resultado = WayPointProgramacao.calcularProgramacao(extremos);

            if (resultado.status !== 'EM_OPERACAO') {
                elemento.innerHTML = `
                    <div class="schedule-heading">
                        <div>
                            <span class="eyebrow">Programação atual</span>
                            <h3>${resultado.status === 'ANTES_OPERACAO' ? 'Operação ainda não iniciada' : 'Operação encerrada'}</h3>
                        </div>
                        <span class="badge">Previsão da linha</span>
                    </div>
                    <p class="muted">${escapeHtml(resultado.mensagem)}</p>
                `;
                return;
            }

            elemento.innerHTML = `
                <div class="schedule-heading">
                    <div>
                        <span class="eyebrow">Programação atual</span>
                        <h3>Trecho previsto da circular</h3>
                    </div>
                    <span class="badge">Estimativa programada</span>
                </div>
                <div class="schedule-route">
                    <div class="schedule-point">
                        <strong>${escapeHtml(resultado.origem.nome)}</strong>
                        <span>${WayPointProgramacao.formatarHora(resultado.origem.minuto)}</span>
                    </div>
                    <div class="schedule-track" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${Math.round(resultado.progresso)}" aria-label="Progresso previsto do trecho">
                        <span class="schedule-progress" style="width:${resultado.progresso}%"></span>
                        <span class="schedule-marker" style="left:${resultado.progresso}%"></span>
                    </div>
                    <div class="schedule-point align-right">
                        <strong>${escapeHtml(resultado.destino.nome)}</strong>
                        <span>${WayPointProgramacao.formatarHora(resultado.destino.minuto)}</span>
                    </div>
                </div>
                <p class="schedule-copy">${escapeHtml(resultado.mensagem)}</p>
            `;
        };

        atualizar();
        estado.programacaoTimer = window.setInterval(atualizar, 60000);
    }

    function atualizarResumoRota(tituloLinha, paradas) {
        const resumo = document.getElementById('rotaResumo');
        if (resumo) {
            resumo.innerHTML = `
                <span class="chip">Linha: ${escapeHtml(tituloLinha)}</span>
                <span class="chip">Sentido: ${escapeHtml(estado.sentidoParadas)}</span>
                <span class="chip">${paradas.length} parada(s)</span>
            `;
        }

        const lista = document.getElementById('listaParadasRota');
        if (lista) {
            lista.innerHTML = paradas
                    .map((parada) => `<li>${escapeHtml(parada.nome || '')}<div class="status-line">Parada ${parada.ordem} - ${escapeHtml(parada.sentido || '')}</div></li>`)
                    .join('');
        }
    }

    function carregarGoogleMapsApi() {
        if (window.google?.maps?.importLibrary) {
            return Promise.resolve();
        }

        if (estado.googleMapsPromise) {
            return estado.googleMapsPromise;
        }

        estado.googleMapsPromise = new Promise((resolve, reject) => {
            window.gm_authFailure = () => {
                const erro = new Error('Google Maps JavaScript API auth failure.');
                erro.codigo = 'AUTH_FAILURE';
                estado.googleMapsPromise = null;
                reject(erro);
            };

            const script = document.createElement('script');
            script.id = estado.googleMapsScriptId;
            const parametros = new URLSearchParams({
                key: googleMapsConfig.apiKey,
                loading: 'async',
                v: 'weekly',
                language: 'pt-BR',
                region: 'BR',
                callback: 'waypointGoogleMapsCarregado'
            });

            window.waypointGoogleMapsCarregado = () => resolve();
            script.src = `https://maps.googleapis.com/maps/api/js?${parametros.toString()}`;
            script.async = true;
            script.onerror = () => {
                estado.googleMapsPromise = null;
                reject(new Error('Google Maps JavaScript API não carregou.'));
            };

            const scriptAntigo = document.getElementById(estado.googleMapsScriptId);
            if (scriptAntigo) {
                scriptAntigo.remove();
            }

            document.head.appendChild(script);
        });

        return estado.googleMapsPromise;
    }

    async function inicializarMapaParadas(paradas) {
        const [{ Map, InfoWindow }] = await Promise.all([
            google.maps.importLibrary('maps'),
            google.maps.importLibrary('marker'),
            google.maps.importLibrary('routes'),
            google.maps.importLibrary('core')
        ]);

        const elementoMapa = document.getElementById('mapaParadas');
        if (!elementoMapa) {
            throw new Error('Elemento HTML do mapa não foi encontrado.');
        }

        const primeiraParada = WayPointMapa.coordenadaParada(paradas[0]);
        if (estado.mapaParadas && estado.mapaParadasElemento === elementoMapa) {
            estado.mapaParadas.setCenter(primeiraParada);
            return;
        }

        estado.mapaParadas = new Map(elementoMapa, {
            center: primeiraParada,
            zoom: 14,
            mapTypeControl: false,
            streetViewControl: false,
            fullscreenControl: true,
            mapId: googleMapsConfig.mapId || 'DEMO_MAP_ID'
        });
        estado.mapaParadasElemento = elementoMapa;
        estado.mapaParadasInfoWindow = new InfoWindow();
    }

    async function desenharRotaParadas(paradas) {
        limparMapaParadas();
        await desenharMarcadoresParadas(paradas);

        if (paradas.length === 1) {
            await ajustarMapaAsParadas(paradas);
            return true;
        }

        try {
            const { Route } = await google.maps.importLibrary('routes');
            const trechos = WayPointMapa.criarTrechosRota(paradas, 27);
            const caminhos = [];

            for (const trecho of trechos) {
                const pontos = trecho.map(WayPointMapa.coordenadaParada);
                const origem = pontos[0];
                const destino = pontos[pontos.length - 1];
                const intermediarios = pontos.slice(1, -1).map((ponto) => ({ location: ponto }));

                const { routes } = await Route.computeRoutes({
                    origin: origem,
                    destination: destino,
                    intermediates: intermediarios,
                    travelMode: 'DRIVING',
                    optimizeWaypointOrder: false,
                    region: 'br',
                    fields: ['path', 'viewport']
                });

                if (!routes || routes.length === 0) {
                    throw new Error('Nenhuma rota foi retornada pelo Google Maps.');
                }

                const polylines = routes[0].createPolylines({
                    polylineOptions: {
                        strokeColor: estado.sentidoParadas === 'IDA' ? '#2A8C4E' : '#1765C1',
                        strokeOpacity: 0.92,
                        strokeWeight: 5
                    }
                });

                polylines.forEach((polyline) => {
                    polyline.setMap(estado.mapaParadas);
                    estado.mapaParadasPolylines.push(polyline);
                });

                if (routes[0].path) {
                    caminhos.push(...routes[0].path);
                }
            }

            await ajustarMapaAoCaminho(caminhos.length > 0 ? caminhos : paradas.map(WayPointMapa.coordenadaParada));
            return true;
        } catch (erro) {
            console.warn('Não foi possível calcular rota por vias. Usando Polyline aproximada.', erro);
            await desenharPolylineAproximada(paradas);
            await ajustarMapaAsParadas(paradas);
            mostrarStatusMapa('Rota aproximada exibida com base nas paradas cadastradas.');
            return false;
        }
    }

    async function desenharPolylineAproximada(paradas) {
        const { Polyline } = await google.maps.importLibrary('maps');
        const polyline = new Polyline({
            path: paradas.map(WayPointMapa.coordenadaParada),
            strokeColor: estado.sentidoParadas === 'IDA' ? '#2A8C4E' : '#1765C1',
            strokeOpacity: 0.72,
            strokeWeight: 4,
            geodesic: false
        });
        polyline.setMap(estado.mapaParadas);
        estado.mapaParadasPolylines.push(polyline);
    }

    async function desenharMarcadoresParadas(paradas) {
        const { AdvancedMarkerElement } = await google.maps.importLibrary('marker');

        paradas.forEach((parada, indice) => {
            const marker = new AdvancedMarkerElement({
                map: estado.mapaParadas,
                position: WayPointMapa.coordenadaParada(parada),
                title: parada.nome,
                gmpClickable: true
            });

            marker.appendChild(WayPointMapa.criarConteudoMarcador(parada, indice, paradas.length));

            marker.addEventListener('gmp-click', () => {
                estado.mapaParadasInfoWindow.setContent(`
                    <strong>${escapeHtml(parada.nome)}</strong><br>
                    Parada ${parada.ordem}<br>
                    Sentido: ${escapeHtml(parada.sentido)}
                `);
                estado.mapaParadasInfoWindow.open({ anchor: marker, map: estado.mapaParadas });
            });

            estado.mapaParadasMarkers.push(marker);
        });
    }

    async function ajustarMapaAsParadas(paradas) {
        await ajustarMapaAoCaminho(paradas.map(WayPointMapa.coordenadaParada));
    }

    async function ajustarMapaAoCaminho(pontos) {
        const { LatLngBounds } = await google.maps.importLibrary('core');
        const bounds = new LatLngBounds();
        pontos.forEach((ponto) => bounds.extend(ponto));
        estado.mapaParadas.fitBounds(bounds);
    }

    function limparMapaParadas() {
        estado.mapaParadasMarkers.forEach((marker) => marker.map = null);
        estado.mapaParadasPolylines.forEach((polyline) => polyline.setMap(null));
        estado.mapaParadasMarkers = [];
        estado.mapaParadasPolylines = [];
    }

    function mostrarStatusMapa(texto) {
        const status = document.getElementById('mapaParadasStatus');
        if (status) {
            status.textContent = texto;
        }
    }

    function atualizarBotoesSentidoParadas() {
        document.getElementById('btnParadasIda')?.classList.toggle('active', estado.sentidoParadas === 'IDA');
        document.getElementById('btnParadasVolta')?.classList.toggle('active', estado.sentidoParadas === 'VOLTA');
    }

    function renderOcorrencias(ocorrencias) {
        const html = tabelaOuVazio(ocorrencias, `
            <table>
                <thead>
                    <tr>
                        <th>Linha</th>
                        <th>Tipo</th>
                        <th>Prioridade</th>
                        <th>Ativa</th>
                        <th>Descrição</th>
                        <th>Data</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${ocorrencias.map((ocorrencia) => `
                        <tr>
                            <td>${escapeHtml(ocorrencia.linhaCodigo || '')} - ${escapeHtml(ocorrencia.linhaNome || '')}</td>
                            <td><strong>${escapeHtml(textoTipoOcorrencia(ocorrencia.tipo))}</strong></td>
                            <td>${escapeHtml(textoPrioridade(ocorrencia.prioridade))}</td>
                            <td>${ocorrencia.ativa ? 'SIM' : 'NÃO'}</td>
                            <td>${escapeHtml(ocorrencia.descricao || '')}</td>
                            <td>${formatarData(ocorrencia.dataHora)}</td>
                            <td>
                                <div class="row-actions">
                                    ${usuarioLogado.admin && ocorrencia.ativa ? `<button class="danger" type="button" onclick="encerrarOcorrencia(${ocorrencia.id})">Encerrar</button>` : ''}
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhuma ocorrência encontrada.');
        document.getElementById('ocorrenciasResultado').innerHTML = html;
    }

    function renderReportes(reportes) {
        const html = tabelaOuVazio(reportes, `
            <table>
                <thead>
                    <tr>
                        <th>Linha</th>
                        <th>Tipo</th>
                        <th>Autor</th>
                        <th>Confiabilidade</th>
                        <th>Confirmações</th>
                        <th>Data</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${reportes.map((reporte) => `
                        <tr>
                            <td>${escapeHtml(reporte.linhaCodigo || '')} - ${escapeHtml(reporte.linhaNome || '')}</td>
                            <td>${escapeHtml(reporte.tipoReporte || '')}</td>
                            <td>${escapeHtml(reporte.autorNome || '')}</td>
                            <td>${formatarConfiabilidade(reporte.autorConfiabilidade)}</td>
                            <td>${progressoConfiabilidade(reporte.quantidadeConfirmacoes ?? 0)}</td>
                            <td>${formatarData(reporte.dataHora)}</td>
                            <td>${acoesReporte(reporte)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhum reporte encontrado.');
        document.getElementById('reportesResultado').innerHTML = html;
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

    function textoTipoOcorrencia(tipo) {
        const textos = {
            ATRASO: 'Atraso',
            DESVIO: 'Desvio',
            VEICULO_QUEBRADO: 'Veículo quebrado',
            LOTACAO: 'Lotação',
            INTERROMPIDA: 'Linha interrompida',
            UNIFICADA: 'Linha unificada'
        };

        return textos[tipo] || tipo || 'Ocorrencia';
    }

    function textoPrioridade(prioridade) {
        const textos = {
            BAIXA: 'Baixa',
            MEDIA: 'Média',
            ALTA: 'Alta'
        };

        return textos[prioridade] || prioridade || 'Média';
    }

    function textoLinhaNotificacao(notificacao) {
        if (!notificacao.linhaId) {
            return 'Aviso geral';
        }

        return `${escapeHtml(notificacao.linhaCodigo || '')} ${escapeHtml(notificacao.linhaNome || '')}`.trim();
    }

    function acoesReporte(reporte) {
        if (usuarioLogado.admin) {
            return '<span class="badge">Acompanhamento</span>';
        }

        if (reporte.autorDoUsuarioAutenticado) {
            return '<span class="badge">Seu reporte</span>';
        }

        if (reporte.confirmadoPeloUsuarioAutenticado) {
            return '<span class="badge">Confirmado</span>';
        }

        return `<button class="success" type="button" data-reporte-id="${reporte.reporteId}" onclick="confirmarReporte(${reporte.reporteId})">Confirmar reporte</button>`;
    }

    function progressoConfiabilidade(quantidadeConfirmacoes) {
        const confirmacoes = Number(quantidadeConfirmacoes) || 0;
        const proximoMarco = Math.floor(confirmacoes / 5) * 5 + 5;

        if (confirmacoes > 0 && confirmacoes % 5 === 0) {
            return `${confirmacoes} confirmação(ões)`;
        }

        return `${confirmacoes}/${proximoMarco} para próximo aumento`;
    }

    function definirBotaoConfirmacao(reporteId, processando) {
        const botao = document.querySelector(`[data-reporte-id="${reporteId}"]`);
        if (!botao) {
            return;
        }

        botao.disabled = processando;
        botao.textContent = processando ? 'Confirmando...' : 'Confirmar reporte';
    }

    function renderFavoritos(favoritos) {
        const html = tabelaOuVazio(favoritos, `
            <table>
                <thead>
                    <tr>
                        <th>Linha</th>
                        <th>Nome</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${favoritos.map((favorito) => `
                        <tr>
                            <td>${escapeHtml(favorito.linhaCodigo || '')}</td>
                            <td>${escapeHtml(favorito.linhaNome || '')}</td>
                            <td><button class="danger" type="button" onclick="desfavoritarLinha(${favorito.linhaId})">Desfavoritar</button></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhuma linha favorita encontrada.');
        document.getElementById('favoritosResultado').innerHTML = html;
    }

    function renderNotificacoesUsuario(atuais, historico) {
        document.getElementById('notificacoesResultado').innerHTML = `
            <div class="card">
                <h3>Notificações atuais</h3>
                ${tabelaNotificacoes(atuais, 'Nenhuma notificação atual encontrada.')}
            </div>
            <div class="card" style="margin-top:14px;">
                <h3>Histórico</h3>
                ${tabelaNotificacoes(historico, 'Nenhuma notificação no histórico.')}
            </div>
        `;
    }

    function tabelaNotificacoes(notificacoes, vazio) {
        return tabelaOuVazio(notificacoes, `
            <table>
                <thead>
                    <tr>
                        <th>Origem</th>
                        <th>Título</th>
                        <th>Mensagem</th>
                        <th>Linha</th>
                        <th>Detalhe</th>
                        <th>Status</th>
                        <th>Lida</th>
                        <th>Data</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${notificacoes.map((notificacao) => `
                        <tr>
                            <td><span class="badge ${notificacao.tipoNotificacao === 'OCORRENCIA' ? 'warning' : ''}">${escapeHtml(notificacao.tipoNotificacao || 'AVISO')}</span></td>
                            <td>${escapeHtml(notificacao.titulo || '')}</td>
                            <td>${escapeHtml(notificacao.mensagem || '')}</td>
                            <td>${textoLinhaNotificacao(notificacao)}</td>
                            <td>${notificacao.tipoNotificacao === 'OCORRENCIA' ? `${escapeHtml(textoTipoOcorrencia(notificacao.tipoOcorrencia))} / ${escapeHtml(textoPrioridade(notificacao.prioridadeOcorrencia))}` : '-'}</td>
                            <td>${escapeHtml(notificacao.status || '')}</td>
                            <td>${notificacao.lida ? 'SIM' : 'NÃO'}</td>
                            <td>${formatarData(notificacao.dataCriacao)}</td>
                            <td>
                                <div class="row-actions">
                                    ${notificacao.lida || notificacao.status === 'SUPERADA' ? '' : `<button class="success" type="button" onclick="marcarNotificacaoLida(${notificacao.id})">Marcar lida</button>`}
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, vazio);
    }

    function renderAvisosAdmin(avisos) {
        const html = tabelaOuVazio(avisos, `
            <table>
                <thead>
                    <tr>
                        <th>Tipo</th>
                        <th>Linha</th>
                        <th>Título</th>
                        <th>Mensagem</th>
                        <th>Status</th>
                        <th>Alcance</th>
                        <th>Origem</th>
                        <th>Data</th>
                        <th>Ações</th>
                    </tr>
                </thead>
                <tbody>
                    ${avisos.map((aviso) => `
                        <tr>
                            <td><span class="badge ${aviso.tipoNotificacao === 'OCORRENCIA' ? 'warning' : ''}">${escapeHtml(aviso.tipoNotificacao || 'AVISO')}</span></td>
                            <td>${textoLinhaNotificacao(aviso)}</td>
                            <td>${escapeHtml(aviso.titulo || '')}</td>
                            <td style="max-width:240px;">${escapeHtml(aviso.mensagem || '')}</td>
                            <td><span class="badge ${aviso.status === 'ATIVA' ? 'success' : ''}">${escapeHtml(aviso.status || '')}</span></td>
                            <td>${aviso.usuariosAlcancados ?? 0}</td>
                            <td>${aviso.ocorrenciaId ? `Ocorrência ${aviso.ocorrenciaId}` : 'Aviso manual'}</td>
                            <td>${formatarData(aviso.dataCriacao)}</td>
                            <td>
                                <div class="row-actions">
                                    ${aviso.status === 'ATIVA'
                                            ? `<button type="button" onclick="marcarAvisoSuperado(${aviso.avisoId ?? 'null'}, ${aviso.referenciaNotificacaoId ?? 'null'})">Marcar como superado</button>`
                                            : '<span class="badge">Superada</span>'}
                                    ${aviso.avisoId
                                            ? `<button class="danger" type="button" data-excluir-aviso-id="${aviso.avisoId}" data-excluir-aviso-titulo="${escapeHtml(aviso.titulo || '')}">Excluir</button>`
                                            : ''}
                                </div>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhum aviso oficial encontrado.');

        const container = document.getElementById('notificacoesResultado');
        container.innerHTML = html;
        container.querySelectorAll('[data-excluir-aviso-id]').forEach((botao) => {
            botao.addEventListener('click', () => {
                excluirAviso(Number(botao.dataset.excluirAvisoId), botao.dataset.excluirAvisoTitulo);
            });
        });
    }

    function renderUsuarios() {
        const container = document.getElementById('usuariosResultado');
        if (!container) {
            return;
        }

        const html = tabelaOuVazio(estado.usuarios, `
            <table>
                <thead>
                    <tr>
                        <th>Nome</th>
                        <th>Email</th>
                        <th>Perfil</th>
                    </tr>
                </thead>
                <tbody>
                    ${estado.usuarios.map((usuario) => `
                        <tr>
                            <td>${escapeHtml(usuario.nome || '')}</td>
                            <td>${escapeHtml(usuario.email || '')}</td>
                            <td><span class="badge ${usuario.tipoUsuario === 'ADMIN' ? 'warning' : 'success'}">${escapeHtml(usuario.tipoUsuario || 'USER')}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `, 'Nenhum usuário encontrado.');
        container.innerHTML = html;
    }

    async function executarAcao(acao) {
        try {
            limparMensagem();
            await acao();
        } catch (erro) {
            mostrarMensagem(erro.message || 'Não foi possível concluir a ação.', 'error');
        }
    }

    async function apiGet(url) {
        return apiRequest(url, { method: 'GET' });
    }

    async function apiJson(url, method, body) {
        const options = { method };
        if (body !== undefined) {
            options.headers = { 'Content-Type': 'application/json' };
            options.body = JSON.stringify(body);
        }
        return apiRequest(url, options);
    }

    async function apiRequest(url, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        let response;
        let data = null;

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
            console.error('Falha de rede ao chamar API', { endpoint: url, metodo: method, erro });
            throw new Error('Não foi possível conectar ao servidor. Verifique se o projeto está rodando.');
        }

        if (response.status !== 204) {
            const contentType = response.headers.get('content-type') || '';
            data = contentType.includes('application/json') ? await response.json() : await response.text();
        }

        if (!response.ok) {
            console.warn('Resposta de erro da API', {
                endpoint: url,
                metodo: method,
                status: response.status,
                corpo: data
            });
            throw new Error(formatarMensagemErro(data, response.status));
        }

        return data;
    }

    function csrfHeaders(method) {
        if (['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
            return {};
        }

        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        return token && header ? { [header]: token } : {};
    }

    function formatarMensagemErro(data, status) {
        const linhas = [];

        if (status === 401 || status === 302) {
            linhas.push('Sua sessao expirou ou o login e necessario. Entre novamente no sistema.');
        }

        if (status === 403) {
            linhas.push('Você não possui permissão para executar esta operação.');
        }

        if (typeof data === 'string') {
            const texto = data.trim();
            if (texto && !texto.toLowerCase().includes('<html')) {
                linhas.push(texto);
            }
        } else if (data && typeof data === 'object') {
            adicionarSeTexto(linhas, data.mensagem);
            adicionarSeTexto(linhas, data.message);
            adicionarSeTexto(linhas, data.erro);
            adicionarSeTexto(linhas, data.error);

            if (data.campos && typeof data.campos === 'object') {
                linhas.push('Verifique os seguintes campos:');
                Object.entries(data.campos).forEach(([campo, mensagem]) => {
                    linhas.push(`- ${campo}: ${formatarValor(mensagem)}`);
                });
            }

            if (linhas.length === 0) {
                linhas.push(formatarValor(data));
            }
        }

        if (linhas.length === 0) {
            linhas.push(`Erro HTTP ${status}. Consulte o Console do navegador para detalhes.`);
        }

        return [...new Set(linhas)].join('\n');
    }

    function adicionarSeTexto(linhas, valor) {
        const texto = formatarValor(valor);
        if (texto) {
            linhas.push(texto);
        }
    }

    function formatarValor(valor) {
        if (valor === null || valor === undefined) {
            return '';
        }

        if (typeof valor === 'string') {
            return valor.trim();
        }

        try {
            return JSON.stringify(valor);
        } catch (erro) {
            return String(valor);
        }
    }

    function tabelaOuVazio(lista, html, vazio) {
        if (!Array.isArray(lista) || lista.length === 0) {
            return `<div class="empty">${vazio}</div>`;
        }

        return `<div class="table-wrap">${html}</div>`;
    }

    function mostrarMensagem(texto, tipo) {
        const mensagem = document.getElementById('mensagem');
        mensagem.textContent = texto;
        mensagem.className = `alert ${tipo}`;
    }

    function limparMensagem() {
        const mensagem = document.getElementById('mensagem');
        mensagem.textContent = '';
        mensagem.className = 'alert';
    }

    function formatarData(valor) {
        if (!valor) {
            return '';
        }

        const data = new Date(valor);
        return Number.isNaN(data.getTime()) ? String(valor).replace('T', ' ') : data.toLocaleString('pt-BR');
    }

    function formatarConfiabilidade(valor) {
        const numero = Number(valor ?? 0);
        return numero.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function escapeHtml(valor) {
        return String(valor ?? '')
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#039;');
    }
