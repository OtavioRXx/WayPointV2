(function () {
    const base = '/img/paradas/';

    const imagens = {
        '1|ida|terminal-centro': 'compartilhadas/terminal-centro.png',
        '1|ida|construcasa': 'vila-brasil/ida/construcasa.png',
        '1|ida|magic-tintas': 'vila-brasil/ida/magic-tintas.png',
        '1|ida|calo': 'vila-brasil/ida/calo.png',
        '1|ida|entrada-rodoviaria': 'vila-brasil/ida/entrada-rodoviaria.png',
        '1|ida|tenis-clube': 'vila-brasil/ida/tenis-clube.png',
        '1|ida|transportadora-floresta': 'vila-brasil/ida/transportadora-floresta.png',
        '1|ida|coelho-style': 'vila-brasil/ida/coelho-style.png',
        '1|ida|entrada-da-vila-brasil': 'vila-brasil/ida/entrada-da-vila-brasil.png',
        '1|ida|vila-brasil': 'vila-brasil/ida/vila-brasil.png',
        '1|volta|vila-brasil': 'vila-brasil/volta/vila-brasil.png',
        '1|volta|takinho-bar': 'vila-brasil/volta/takinho-bar.png',
        '1|volta|mario-pastel': 'vila-brasil/ida/entrada-da-vila-brasil.png',
        '1|volta|rodoviaria': 'vila-brasil/volta/rodoviaria.png',
        '1|volta|virginia': 'vila-brasil/volta/virginia.png',
        '1|volta|point-esfihas': 'vila-brasil/volta/point-esfihas.png',
        '1|volta|terminal-centro': 'compartilhadas/terminal-centro.png',

        '2|ida|terminal-centro': 'compartilhadas/terminal-centro.png',
        '2|ida|construcasa': 'fatec/ida/construcasa.png',
        '2|ida|specialista-tintas': 'fatec/ida/specialista-tintas.png',
        '2|ida|associacao-mirim': 'fatec/ida/associacao-mirim.png',
        '2|ida|assembleia-de-deus': 'fatec/ida/assembleia-de-deus.png',
        '2|ida|r-barao-do-rio-branco': 'fatec/ida/r-barao-do-rio-branco.png',
        '2|ida|r-vitorio-christoni': 'fatec/ida/r-vitorio-christoni.png',
        '2|ida|upa': 'fatec/ida/upa.png',
        '2|ida|ambulatorio-santa-casa': 'fatec/ida/ambulatorio-santa-casa.png',
        '2|ida|av-vitalina-marcusso': 'fatec/ida/av-vitalina-marcusso.png',
        '2|ida|fatec': 'fatec/ida/fatec.png',
        '2|ida|ame': 'fatec/ida/ame.png',
        '2|volta|ame': 'fatec/ida/ame.png',
        '2|volta|sesi': 'fatec/volta/sesi.png',
        '2|volta|maitan': 'fatec/volta/maitan.png',
        '2|volta|upa': 'fatec/volta/upa.png',
        '2|volta|restaurante-vila-beer': 'fatec/volta/restaurante-vila-beer.png',
        '2|volta|r-vitorio-christoni': 'fatec/volta/r-vitorio-christoni.png',
        '2|volta|tapecaria-avenida': 'fatec/volta/tapecaria-avenida.png',
        '2|volta|espanhol-industria-mecanica': 'fatec/volta/espanhol-industria-mecanica.png',
        '2|volta|av-jacinto-ferriera-de-sa': 'fatec/volta/av-jacinto-ferriera-de-sa.png',
        '2|volta|terminal-centro': 'compartilhadas/terminal-centro.png',

        '3|ida|terminal-centro': 'compartilhadas/terminal-centro.png',
        '3|ida|banco-caixa': 'vila-musa/ida/banco-caixa.png',
        '3|ida|jardim-eden': 'vila-musa/ida/jardim-eden.png',
        '3|ida|autopark': 'vila-musa/ida/autopark.png',
        '3|ida|yabadu-pizzaria': 'vila-musa/ida/yabadu-pizzaria.png',
        '3|ida|rua-padre-rui-candido-da-silva': 'vila-musa/ida/rua-padre-rui-candido-da-silva.png',
        '3|ida|paroquia-santo-antonio': 'vila-musa/ida/paroquia-santo-antonio.png',
        '3|ida|r-antonio-bertagnoli': 'vila-musa/ida/r-antonio-bertagnoli.png',
        '3|ida|assembleia-de-deus-ministerio-nova-alianca': 'vila-musa/ida/assembleia-de-deus-ministerio-nova-alianca.png',
        '3|ida|supermercado-sao-judas-tadeu': 'vila-musa/ida/supermercado-sao-judas-tadeu.png',
        '3|ida|r-achiles-zamboni': 'vila-musa/ida/r-achiles-zamboni.png',
        '3|volta|r-achiles-zamboni': 'vila-musa/ida/r-achiles-zamboni.png',
        '3|volta|r-cap-n-marcelino-da-silva': 'vila-musa/volta/r-cap-n-marcelino-da-silva.png',
        '3|volta|supermercado-sao-judas-tadeu': 'vila-musa/volta/supermercado-sao-judas-tadeu.png',
        '3|volta|capela-santa-maria': 'vila-musa/volta/capela-santa-maria.png',
        '3|volta|supermercado-avenida': 'vila-musa/volta/supermercado-avenida.png',
        '3|volta|ee-maria-do-carmo': 'vila-musa/volta/ee-maria-do-carmo.png',
        '3|volta|microtec': 'vila-musa/volta/microtec.png',
        '3|volta|casa-do-chopp': 'vila-musa/volta/casa-do-chopp.png',
        '3|volta|sebrae': 'vila-musa/volta/sebrae.png',
        '3|volta|terminal-centro': 'compartilhadas/terminal-centro.png',

        '4|ida|terminal-centro': 'compartilhadas/terminal-centro.png',
        '4|ida|praca-dos-ferroviarios': 'distrito-industrial/ida/praca-dos-ferroviarios.png',
        '4|ida|bombeiro': 'distrito-industrial/ida/bombeiro.png',
        '4|ida|etec-jacinto-ferreira-de-sa': 'distrito-industrial/ida/etec-jacinto-ferreira-de-sa.png',
        '4|ida|clinica-pontara': 'distrito-industrial/ida/clinica-pontara.png',
        '4|ida|junior-carcare': 'distrito-industrial/ida/junior-carcare.png',
        '4|ida|lagoa-unimed': 'distrito-industrial/ida/lagoa-unimed.png',
        '4|ida|unimed': 'distrito-industrial/ida/unimed.png',
        '4|ida|estacio-ourinhos': 'distrito-industrial/ida/estacio-ourinhos.png',
        '4|ida|toyota': 'distrito-industrial/ida/toyota.png',
        '4|ida|crea-sp': 'distrito-industrial/ida/crea-sp.png',
        '4|ida|castor': 'distrito-industrial/ida/castor.png',
        '4|ida|cwa-industria-mecanica': 'distrito-industrial/ida/cwa-industria-mecanica.png',
        '4|volta|cwa-industria-mecanica': 'distrito-industrial/ida/cwa-industria-mecanica.png',
        '4|volta|mestra-artesanal': 'distrito-industrial/volta/mestra-artesanal.png',
        '4|volta|cafe-jaguari': 'distrito-industrial/volta/cafe-jaguari.png',
        '4|volta|emei-itaipava': 'distrito-industrial/volta/emei-itaipava.png',
        '4|volta|ceep': 'distrito-industrial/volta/ceep.png',
        '4|volta|coletto-3r-ourinhos': 'distrito-industrial/volta/coletto-3r-ourinhos.png',
        '4|volta|ponto-unimed': 'distrito-industrial/volta/ponto-unimed.png',
        '4|volta|pao-de-acucar': 'distrito-industrial/volta/pao-de-acucar.png',
        '4|volta|etec-jacinto-ferreiro-de-sa': 'distrito-industrial/volta/etec-jacinto-ferreiro-de-sa.png',
        '4|volta|lec-idiomas': 'distrito-industrial/volta/lec-idiomas.png',
        '4|volta|farmacias-nissei': 'distrito-industrial/volta/farmacias-nissei.png',
        '4|volta|terminal-centro': 'compartilhadas/terminal-centro.png'
    };

    function normalizar(valor) {
        return String(valor || '')
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/ç/g, 'c')
                .replace(/Ç/g, 'c')
                .toLowerCase()
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-+|-+$/g, '');
    }

    function chave(linhaId, parada) {
        return `${linhaId}|${normalizar(parada.sentido)}|${normalizar(parada.nome)}`;
    }

    function resolver(linhaId, parada) {
        const caminho = imagens[chave(linhaId, parada)];

        if (!caminho) {
            return null;
        }

        return {
            src: `${base}${caminho}`,
            alt: `Foto da parada ${parada.nome}`
        };
    }

    window.WayPointParadaImagens = {
        resolver,
        normalizar
    };
})();
