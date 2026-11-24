(function (global) {
    const CONFIG_PADRAO = Object.freeze({
        horaInicial: '06:00',
        horaFinal: '22:30',
        duracaoTrechoMinutos: 20,
        terminal: 'Terminal',
        bairro: 'Bairro'
    });

    function minutosDoDia(horario) {
        if (typeof horario === 'number') {
            return horario;
        }

        const partes = String(horario || '').split(':').map(Number);
        if (partes.length < 2 || partes.some((parte) => Number.isNaN(parte))) {
            throw new Error(`Horário inválido: ${horario}`);
        }

        return partes[0] * 60 + partes[1];
    }

    function formatarHora(totalMinutos) {
        const minutosNormalizados = Math.max(0, Math.floor(totalMinutos));
        const horas = Math.floor(minutosNormalizados / 60);
        const minutos = minutosNormalizados % 60;
        return `${String(horas).padStart(2, '0')}:${String(minutos).padStart(2, '0')}`;
    }

    function criarPontosProgramados(config = {}) {
        const opcoes = { ...CONFIG_PADRAO, ...config };
        const inicio = minutosDoDia(opcoes.horaInicial);
        const fim = minutosDoDia(opcoes.horaFinal);
        const duracao = Number(opcoes.duracaoTrechoMinutos);

        if (!Number.isFinite(duracao) || duracao <= 0) {
            throw new Error('Duração do trecho deve ser maior que zero.');
        }

        const pontos = [];
        let minutoAtual = inicio;
        let indice = 0;

        while (minutoAtual < fim) {
            pontos.push({
                nome: indice % 2 === 0 ? opcoes.terminal : opcoes.bairro,
                minuto: minutoAtual
            });
            minutoAtual += duracao;
            indice += 1;
        }

        const ultimoPonto = pontos[pontos.length - 1];
        if (!ultimoPonto || ultimoPonto.minuto !== fim || ultimoPonto.nome !== opcoes.terminal) {
            pontos.push({
                nome: opcoes.terminal,
                minuto: fim,
                excecaoFinal: ultimoPonto ? fim - ultimoPonto.minuto !== duracao : false
            });
        }

        return pontos;
    }

    function obterMinutoAtual(agora) {
        if (typeof agora === 'string') {
            return minutosDoDia(agora);
        }

        if (typeof agora === 'number') {
            return agora;
        }

        const data = agora instanceof Date ? agora : new Date();
        return data.getHours() * 60 + data.getMinutes();
    }

    function calcularProgramacao(config = {}) {
        const opcoes = { ...CONFIG_PADRAO, ...config };
        const pontos = criarPontosProgramados(opcoes);
        const inicio = minutosDoDia(opcoes.horaInicial);
        const fim = minutosDoDia(opcoes.horaFinal);
        const agoraMinutos = obterMinutoAtual(opcoes.agora);

        if (agoraMinutos < inicio) {
            return {
                status: 'ANTES_OPERACAO',
                progresso: 0,
                pontos,
                mensagem: `Operação ainda não iniciada. Primeira saída de ${opcoes.terminal} às ${opcoes.horaInicial}.`,
                proximos: pontos.slice(0, 3)
            };
        }

        if (agoraMinutos >= fim) {
            return {
                status: 'ENCERRADA',
                progresso: 100,
                pontos,
                mensagem: `Operação encerrada. A circulação será retomada às ${opcoes.horaInicial}.`,
                proximos: []
            };
        }

        const indiceOrigem = Math.max(0, pontos.findIndex((ponto, indice) => {
            const proximo = pontos[indice + 1];
            return proximo && agoraMinutos >= ponto.minuto && agoraMinutos < proximo.minuto;
        }));
        const origem = pontos[indiceOrigem];
        const destino = pontos[indiceOrigem + 1];
        const duracaoTrecho = destino.minuto - origem.minuto;
        const progresso = Math.min(100, Math.max(0, ((agoraMinutos - origem.minuto) / duracaoTrecho) * 100));

        return {
            status: 'EM_OPERACAO',
            origem,
            destino,
            progresso,
            excecaoFinal: Boolean(destino.excecaoFinal),
            pontos,
            proximos: pontos.slice(indiceOrigem + 1, indiceOrigem + 4),
            mensagem: `A circular saiu de ${origem.nome} às ${formatarHora(origem.minuto)} e está prevista para chegar em ${destino.nome} às ${formatarHora(destino.minuto)}. Esta informação é uma previsão baseada na programação da linha.`
        };
    }

    function extremosDaRota(paradas, sentido = 'IDA') {
        const ordenadas = Array.isArray(paradas)
                ? [...paradas].sort((a, b) => Number(a.ordem) - Number(b.ordem))
                : [];

        const primeira = ordenadas[0]?.nome || CONFIG_PADRAO.terminal;
        const ultima = ordenadas[ordenadas.length - 1]?.nome || CONFIG_PADRAO.bairro;

        return sentido === 'VOLTA'
                ? { terminal: ultima, bairro: primeira }
                : { terminal: primeira, bairro: ultima };
    }

    const api = {
        CONFIG_PADRAO,
        minutosDoDia,
        formatarHora,
        criarPontosProgramados,
        calcularProgramacao,
        extremosDaRota
    };

    global.WayPointProgramacao = api;

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
})(typeof window !== 'undefined' ? window : globalThis);
