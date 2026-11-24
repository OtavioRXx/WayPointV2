(function () {
    const autenticado = document.body.dataset.authenticated === 'true' && Number(document.body.dataset.userId) > 0;
    const linhaSelect = document.getElementById('linhaSelect');
    const lista = document.getElementById('paradasLista');
    const detalhe = document.getElementById('detalheParada');
    const mapaElemento = document.getElementById('mapaParadas');
    const statusMapa = document.getElementById('mapaParadasStatus');
    const programacaoElemento = document.getElementById('programacaoAtual');
    const configMaps = {
        apiKey: document.body.dataset.googleMapsApiKey || '',
        mapId: document.body.dataset.googleMapsMapId || 'DEMO_MAP_ID'
    };
    const parametrosUrl = new URLSearchParams(window.location.search);
    const linhaInicialId = parametrosUrl.get('linhaId');

    let linhas = [];
    let paradas = [];
    let sentido = 'IDA';
    let mapa = null;
    let markers = [];
    let polylines = [];
    let infoWindow = null;
    let mapsPromise = null;
    let paradaSelecionada = null;
    let programacaoTimer = null;

    document.addEventListener('DOMContentLoaded', async () => {
        linhaSelect?.addEventListener('change', carregarRota);
        document.querySelectorAll('[data-sentido]').forEach((botao) => {
            botao.addEventListener('click', () => selecionarSentido(botao.dataset.sentido));
        });
        await carregarLinhas();
    });

    async function carregarLinhas() {
        await WayPoint.executar(async () => {
            linhas = await WayPoint.get('/api/linhas');
            WayPoint.popularSelectLinhas(linhaSelect, linhas);
            if (linhas.length) {
                const linhaInicial = linhas.some((linha) => String(linha.id) === String(linhaInicialId))
                        ? linhaInicialId
                        : linhas[0].id;
                sentido = 'IDA';
                document.getElementById('btnIda')?.classList.add('active');
                document.getElementById('btnVolta')?.classList.remove('active');
                linhaSelect.value = linhaInicial;
                await carregarRota();
            } else {
                lista.innerHTML = '<div class="empty">Nenhuma linha cadastrada.</div>';
            }
        });
    }

    async function selecionarSentido(novoSentido) {
        sentido = novoSentido;
        document.getElementById('btnIda')?.classList.toggle('active', sentido === 'IDA');
        document.getElementById('btnVolta')?.classList.toggle('active', sentido === 'VOLTA');
        await carregarRota();
    }

    async function carregarRota() {
        const linhaId = linhaSelect.value;
        if (!linhaId) {
            return;
        }

        await WayPoint.executar(async () => {
            WayPoint.mostrarCarregando(lista, 'Carregando paradas');
            paradas = await WayPoint.get(`/api/linhas/${linhaId}/paradas?sentido=${sentido}`);
            limparSelecao();
            renderLista();
            renderProgramacaoAtual();
            renderMapa().catch((erro) => {
                console.error('Erro não tratado ao renderizar mapa', erro);
                statusMapa.textContent = 'Não foi possível carregar o mapa. A lista de paradas continua disponível.';
            });
        });
    }

    function renderLista() {
        if (!paradas.length) {
            lista.innerHTML = `<div class="empty">Nenhuma parada cadastrada para o sentido ${sentido}.</div>`;
            renderProgramacaoAtual();
            return;
        }

        lista.innerHTML = paradas.map((parada, indice) => `
            <article class="route-card${paradaSelecionada?.id === parada.id ? ' selected' : ''}" data-card-parada-id="${parada.id}">
                <span class="route-order">${indice + 1}</span>
                ${renderFotoParada(parada)}
                <div class="route-info">
                    <strong>${WayPoint.escapeHtml(parada.nome)}</strong>
                    <p class="muted">Sentido: ${WayPoint.escapeHtml(parada.sentido)} | Ordem oficial: ${parada.ordem}</p>
                </div>
                <div class="route-actions">
                        <button class="primary" type="button" data-reportar-parada-id="${parada.id}">${autenticado ? 'Reportar situação' : 'Detalhes da parada'}</button>
                </div>
            </article>
        `).join('');

        lista.querySelectorAll('button[data-reportar-parada-id]').forEach((botao) => {
            botao.addEventListener('click', () => selecionarParada(Number(botao.dataset.reportarParadaId)));
        });

        lista.querySelectorAll('[data-parada-img]').forEach((imagem) => {
            imagem.addEventListener('error', () => {
                const moldura = imagem.closest('.route-photo');
                imagem.remove();
                moldura?.classList.add('is-missing');
            }, { once: true });
        });
    }

    function renderFotoParada(parada) {
        const imagem = window.WayPointParadaImagens?.resolver(linhaSelect.value, parada);
        const nome = WayPoint.escapeHtml(parada.nome);

        if (!imagem) {
            return `
                <figure class="route-photo is-missing" aria-label="Foto da parada não disponível">
                    <span>Sem foto</span>
                </figure>
            `;
        }

        return `
            <figure class="route-photo">
                <img
                    src="${WayPoint.escapeHtml(imagem.src)}"
                    alt="${WayPoint.escapeHtml(imagem.alt)}"
                    loading="lazy"
                    data-parada-img="${nome}">
                <span>Sem foto</span>
            </figure>
        `;
    }

    function selecionarParada(paradaId, rolarAtePainel = true) {
        const parada = paradas.find((item) => Number(item.id) === paradaId);
        if (!parada) {
            WayPoint.mostrarMensagem('Parada selecionada não foi encontrada nesta rota.', 'error');
            return;
        }

        paradaSelecionada = parada;
        atualizarCardSelecionado();
        renderPainelParada(parada);

        if (rolarAtePainel) {
            detalhe.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function renderPainelParada(parada) {
        const linha = linhas.find((item) => String(item.id) === String(linhaSelect.value));

        detalhe.classList.remove('hidden');
        detalhe.innerHTML = `
            <div class="panel-header">
                <div>
                    <span class="badge">Parada selecionada</span>
                    <h2>Parada ${parada.ordem} - ${WayPoint.escapeHtml(parada.nome)}</h2>
                    <p class="muted">${WayPoint.escapeHtml(linha.codigo)} - ${WayPoint.escapeHtml(linha.nome)} | Sentido: ${WayPoint.escapeHtml(parada.sentido)}</p>
                </div>
            </div>
            ${autenticado ? `
                <div class="report-panel">
                    <h3>O que está acontecendo nesta parada?</h3>
                    <p class="muted">Escolha uma opção para contribuir com outros passageiros. O horário programado fica separado abaixo do mapa.</p>
                    <div class="report-actions">
                        <button class="success report-choice" type="button" data-tipo="PASSOU">O ônibus passou</button>
                        <button class="report-choice warning" type="button" data-tipo="ATRASADO">Está atrasado</button>
                        <button class="report-choice" type="button" data-tipo="LOTADO">Está lotado</button>
                        <button class="danger report-choice" type="button" data-tipo="NAO_PASSOU">Ainda não passou</button>
                    </div>
                    <div id="reporteFeedback" class="inline-feedback hidden"></div>
                </div>
            ` : `
                <div class="report-panel visitor-report-gate">
                    <h3>Quer informar a situação desta parada?</h3>
                    <p class="muted">O envio de reportes colaborativos exige uma conta autenticada para garantir a confiabilidade das informações.</p>
                    <div class="visitor-banner-actions">
                        <a class="wp-button primary" href="/login">Entrar para reportar</a>
                        <a class="wp-button ghost" href="/cadastro">Criar conta</a>
                    </div>
                </div>
            `}
        `;

        if (autenticado) {
            detalhe.querySelectorAll('button[data-tipo]').forEach((botao) => {
                botao.addEventListener('click', () => criarReporte(botao.dataset.tipo));
            });
        }
    }

    function atualizarCardSelecionado() {
        lista.querySelectorAll('[data-card-parada-id]').forEach((card) => {
            card.classList.toggle(
                    'selected',
                    Number(card.dataset.cardParadaId) === Number(paradaSelecionada?.id)
            );
        });
    }

    function limparSelecao() {
        paradaSelecionada = null;
        detalhe.classList.add('hidden');
        detalhe.innerHTML = '';
    }

    async function criarReporte(tipoReporte) {
        if (!paradaSelecionada) {
            WayPoint.mostrarMensagem('Selecione uma parada antes de enviar o reporte.', 'error');
            return;
        }

        const parada = paradaSelecionada;
        const linha = linhas.find((item) => String(item.id) === String(linhaSelect.value));
        const botoes = [...detalhe.querySelectorAll('button[data-tipo]')];
        const botaoClicado = detalhe.querySelector(`button[data-tipo="${tipoReporte}"]`);
        const feedback = document.getElementById('reporteFeedback');
        const textoOriginal = botaoClicado?.textContent || '';

        botoes.forEach((botao) => botao.disabled = true);
        if (botaoClicado) {
            botaoClicado.classList.add('selected');
            botaoClicado.textContent = 'Enviando...';
        }
        feedback.className = 'inline-feedback';
        feedback.textContent = 'Enviando reporte...';

        try {
            const resposta = await WayPoint.json('/api/reportes/minhas/paradas', 'POST', {
                paradaId: parada.id,
                tipoReporte
            });
            const horario = WayPoint.formatarData(resposta.dataHora);
            feedback.className = 'inline-feedback ok';
            feedback.textContent = `Reporte enviado com sucesso. Você informou que a linha ${linha.codigo} - ${linha.nome} ${textoTipoReporte(tipoReporte).toLowerCase()} na parada ${parada.nome} em ${horario}.`;
            if (botaoClicado) {
                botaoClicado.textContent = 'Reporte enviado';
            }
            WayPoint.mostrarMensagem('Reporte enviado com sucesso.', 'ok');
        } catch (erro) {
            console.error('Falha ao enviar reporte da parada', {
                paradaId: parada.id,
                tipoReporte,
                erro
            });
            feedback.className = 'inline-feedback error';
            feedback.textContent = `Não foi possível enviar o reporte. Motivo: ${erro.message || 'erro inesperado'}`;
        } finally {
            botoes.forEach((botao) => botao.disabled = false);
            if (botaoClicado) {
                window.setTimeout(() => {
                    botaoClicado.classList.remove('selected');
                    botaoClicado.textContent = textoOriginal;
                }, 1200);
            }
        }
    }

    function textoTipoReporte(tipoReporte) {
        const textos = {
            PASSOU: 'O ônibus passou',
            ATRASADO: 'O ônibus está atrasado',
            NAO_PASSOU: 'O ônibus ainda não passou',
            LOTADO: 'O ônibus passou lotado'
        };

        return textos[tipoReporte] || tipoReporte;
    }

    function renderProgramacaoAtual() {
        if (!programacaoElemento) {
            return;
        }

        if (programacaoTimer) {
            window.clearInterval(programacaoTimer);
            programacaoTimer = null;
        }

        if (!paradas.length) {
            programacaoElemento.innerHTML = '<div class="empty">Nenhuma parada cadastrada para calcular a programação desta rota.</div>';
            return;
        }

        atualizarProgramacaoAtual();
        programacaoTimer = window.setInterval(atualizarProgramacaoAtual, 60000);
    }

    function atualizarProgramacaoAtual() {
        if (!programacaoElemento || !window.WayPointProgramacao) {
            return;
        }

        const extremos = WayPointProgramacao.extremosDaRota(paradas, sentido);
        const resultado = WayPointProgramacao.calcularProgramacao(extremos);

        if (resultado.status !== 'EM_OPERACAO') {
            programacaoElemento.innerHTML = `
                <div class="schedule-heading">
                    <div>
                        <span class="eyebrow">Programação atual</span>
                        <h3>${resultado.status === 'ANTES_OPERACAO' ? 'Operação ainda não iniciada' : 'Operação encerrada'}</h3>
                    </div>
                    <span class="badge">Previsão da linha</span>
                </div>
                <p class="muted">${WayPoint.escapeHtml(resultado.mensagem)}</p>
            `;
            return;
        }

        programacaoElemento.innerHTML = `
            <div class="schedule-heading">
                <div>
                    <span class="eyebrow">Programação atual</span>
                    <h3>Trecho previsto da circular</h3>
                </div>
                <span class="badge">Estimativa programada</span>
            </div>
            <div class="schedule-route">
                <div class="schedule-point">
                    <strong>${WayPoint.escapeHtml(resultado.origem.nome)}</strong>
                    <span>${WayPointProgramacao.formatarHora(resultado.origem.minuto)}</span>
                </div>
                <div class="schedule-track" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${Math.round(resultado.progresso)}" aria-label="Progresso previsto do trecho">
                    <span class="schedule-progress" style="width:${resultado.progresso}%"></span>
                    <span class="schedule-marker" style="left:${resultado.progresso}%"></span>
                </div>
                <div class="schedule-point align-right">
                    <strong>${WayPoint.escapeHtml(resultado.destino.nome)}</strong>
                    <span>${WayPointProgramacao.formatarHora(resultado.destino.minuto)}</span>
                </div>
            </div>
            <p class="schedule-copy">${WayPoint.escapeHtml(resultado.mensagem)}</p>
            ${renderProximosPontos(resultado.proximos)}
        `;
    }

    function renderProximosPontos(proximos) {
        if (!Array.isArray(proximos) || proximos.length === 0) {
            return '';
        }

        return `
            <div class="schedule-next">
                <span>Próximos pontos previstos</span>
                ${proximos.map((ponto) => `<strong>${WayPoint.escapeHtml(ponto.nome)} ${WayPointProgramacao.formatarHora(ponto.minuto)}</strong>`).join('')}
            </div>
        `;
    }

    async function renderMapa() {
        limparMapa();
        statusMapa.textContent = '';

        if (!paradas.length) {
            return;
        }

        const paradasValidas = validarParadasParaMapa(paradas);
        if (!paradasValidas.length) {
            statusMapa.textContent = 'Nenhuma parada com coordenadas válidas para desenhar no mapa.';
            return;
        }

        if (!configMaps.apiKey) {
            statusMapa.textContent = 'Google Maps não configurado. As paradas continuam disponíveis na lista.';
            return;
        }

        try {
            await carregarGoogleMaps();
            const { Map, Polyline, InfoWindow } = await google.maps.importLibrary('maps');
            const { LatLngBounds } = await google.maps.importLibrary('core');
            const { AdvancedMarkerElement } = await google.maps.importLibrary('marker');

            if (!mapa) {
                mapa = new Map(mapaElemento, {
                    zoom: 14,
                    center: WayPointMapa.coordenadaParada(paradasValidas[0]),
                    mapId: configMaps.mapId
                });
                infoWindow = new InfoWindow();
            }

            const bounds = new LatLngBounds();

            paradasValidas.forEach((parada, indice) => {
                const conteudo = WayPointMapa.criarConteudoMarcador(parada, indice, paradasValidas.length);
                const marker = new AdvancedMarkerElement({
                    map: mapa,
                    position: WayPointMapa.coordenadaParada(parada),
                    title: parada.nome,
                    content: conteudo
                });
                marker.addEventListener('gmp-click', () => {
                    infoWindow.setContent(`<strong>${WayPoint.escapeHtml(parada.nome)}</strong><br>Parada ${parada.ordem}<br>Sentido: ${WayPoint.escapeHtml(parada.sentido)}`);
                    infoWindow.open({ anchor: marker, map: mapa });
                    selecionarParada(parada.id);
                });
                markers.push(marker);
                bounds.extend(marker.position);
            });

            const polylineAproximada = new Polyline({
                path: paradasValidas.map((parada) => WayPointMapa.coordenadaParada(parada)),
                strokeColor: sentido === 'IDA' ? '#56C47C' : '#5B9FE6',
                strokeOpacity: 0.8,
                strokeWeight: 4
            });
            polylineAproximada.setMap(mapa);
            polylines.push(polylineAproximada);
            mapa.fitBounds(bounds);

            if (paradasValidas.length > 1) {
                await tentarDesenharRotaPelasVias(paradasValidas, Polyline);
            }
        } catch (erro) {
            console.error('Falha ao carregar Google Maps', erro);
            statusMapa.textContent = WayPointMapa.mensagemErroMaps(erro);
        }
    }

    function validarParadasParaMapa(listaParadas) {
        console.info(`Paradas recebidas para ${sentido}: ${listaParadas.length}`);
        console.info(listaParadas.map((parada) => ({
            id: parada.id,
            ordem: parada.ordem,
            latitude: parada.latitude,
            longitude: parada.longitude,
            sentido: parada.sentido
        })));

        const paradasOrdenadas = [...listaParadas].sort((a, b) => Number(a.ordem) - Number(b.ordem));
        const invalidas = [];

        const validas = paradasOrdenadas.filter((parada) => {
            const latitude = Number(parada.latitude);
            const longitude = Number(parada.longitude);
            const valida = Number.isFinite(latitude)
                    && Number.isFinite(longitude)
                    && latitude >= -90
                    && latitude <= 90
                    && longitude >= -180
                    && longitude <= 180
                    && parada.sentido === sentido;

            if (!valida) {
                invalidas.push(parada);
            }

            return valida;
        });

        if (invalidas.length) {
            console.warn('Paradas ignoradas por coordenadas ou sentido inválidos', invalidas.map((parada) => ({
                id: parada.id,
                nome: parada.nome,
                ordem: parada.ordem,
                latitude: parada.latitude,
                longitude: parada.longitude,
                sentido: parada.sentido
            })));
            statusMapa.textContent = `Algumas paradas possuem coordenadas inválidas e não foram enviadas ao Google Maps: ${invalidas.map((parada) => parada.nome).join(', ')}.`;
        }

        return validas;
    }

    async function tentarDesenharRotaPelasVias(paradasValidas, Polyline) {
        try {
            const { Route } = await google.maps.importLibrary('routes');
            const trechos = WayPointMapa.criarTrechosRota(paradasValidas, 27);
            const polylinesPorVias = [];

            for (const trecho of trechos) {
                const pontos = trecho.map(WayPointMapa.coordenadaParada);
                const origem = pontos[0];
                const destino = pontos[pontos.length - 1];
                const intermediarios = pontos.slice(1, -1).map((ponto) => ({
                    location: ponto,
                    vehicleStopover: true
                }));

                const resposta = await Route.computeRoutes({
                    origin: origem,
                    destination: destino,
                    intermediates: intermediarios,
                    travelMode: 'DRIVING',
                    optimizeWaypointOrder: false,
                    region: 'br',
                    fields: ['path']
                });

                const rota = resposta.routes?.[0];
                if (!rota?.path?.length) {
                    throw new Error('O Google Maps não retornou path para a rota.');
                }

                const polyline = new Polyline({
                    path: rota.path,
                    strokeColor: sentido === 'IDA' ? '#2A8C4E' : '#1765C1',
                    strokeOpacity: 0.95,
                    strokeWeight: 5
                });
                polylinesPorVias.push(polyline);
            }

            polylines.forEach((polyline) => polyline.setMap(null));
            polylines = [];
            polylinesPorVias.forEach((polyline) => {
                polyline.setMap(mapa);
                polylines.push(polyline);
            });
            if (!statusMapa.textContent.startsWith('Algumas paradas possuem coordenadas inválidas')) {
                statusMapa.textContent = '';
            }
        } catch (erro) {
            console.warn('Não foi possível calcular o percurso pelas vias. Mantendo Polyline aproximada.', erro);
            statusMapa.textContent = 'Não foi possível calcular o percurso pelas vias. Exibindo rota aproximada com base nas paradas cadastradas.';
        }
    }

    function carregarGoogleMaps() {
        if (window.google?.maps) {
            return Promise.resolve();
        }

        if (mapsPromise) {
            return mapsPromise;
        }

        mapsPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(configMaps.apiKey)}&v=weekly&libraries=marker&map_ids=${encodeURIComponent(configMaps.mapId)}`;
            script.async = true;
            script.onload = resolve;
            script.onerror = () => reject(new Error('Não foi possível carregar o script do Google Maps.'));
            document.head.appendChild(script);
        });

        return mapsPromise;
    }

    function limparMapa() {
        markers.forEach((marker) => marker.map = null);
        markers = [];

        polylines.forEach((item) => item.setMap(null));
        polylines = [];
    }
})();
