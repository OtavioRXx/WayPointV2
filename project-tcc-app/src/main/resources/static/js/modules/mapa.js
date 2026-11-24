(function () {
    function coordenadaParada(parada) {
        return {
            lat: Number(parada.latitude),
            lng: Number(parada.longitude)
        };
    }

    function criarTrechosRota(paradas, limitePontos) {
        const trechos = [];
        let inicio = 0;

        while (inicio < paradas.length - 1) {
            const fim = Math.min(inicio + limitePontos - 1, paradas.length - 1);
            trechos.push(paradas.slice(inicio, fim + 1));
            inicio = fim;
        }

        return trechos;
    }

    function criarConteudoMarcador(parada, indice, total) {
        const marcador = document.createElement('div');
        marcador.className = 'map-pin';
        marcador.textContent = indice === 0 ? 'I' : indice === total - 1 ? 'F' : String(parada.ordem);
        marcador.style.background = indice === 0 ? '#2A8C4E' : indice === total - 1 ? '#0B2239' : '#1765C1';
        return marcador;
    }

    function mensagemErroMaps(erro) {
        const mensagem = erro?.message || '';
        const codigo = erro?.codigo || mensagem.match(/(?:error|warning):\s*([A-Za-z0-9]+MapError|[A-Za-z0-9]+)/)?.[1];

        if (codigo === 'AUTH_FAILURE') {
            return 'A chave da API não foi autorizada. Verifique a Demo Key, referrer e APIs habilitadas.';
        }

        if (mensagem.includes('MissingKeyMapError')) {
            return 'A chave da API não foi enviada ao Google Maps.';
        }

        if (mensagem.includes('RefererNotAllowedMapError')) {
            return 'A chave da API não permite este endereço. Autorize http://localhost:8081/* e http://localhost:8080/*.';
        }

        if (mensagem.includes('InvalidKeyMapError')) {
            return 'A chave da API é inválida ou não foi encontrada pelo Google Maps.';
        }

        if (mensagem.includes('ApiNotActivatedMapError')) {
            return 'A Maps JavaScript API não está habilitada para esta chave.';
        }

        if (mensagem.includes('BillingNotEnabledMapError')) {
            return 'A chave exige uma configuração de faturamento ativa no Google Cloud.';
        }

        return 'Não foi possível carregar o Google Maps. Verifique a configuração da Demo Key.';
    }

    window.WayPointMapa = {
        coordenadaParada,
        criarTrechosRota,
        criarConteudoMarcador,
        mensagemErroMaps
    };
})();
