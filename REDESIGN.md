# Redesign visual do WayPoint

Redesign completo da camada de apresentação. **Nenhum arquivo Java, SQL, `pom.xml`, `application*.properties` ou de teste foi alterado.** Rotas, endpoints, autenticação, permissões, nomes de campos e regras de negócio continuam exatamente como estavam.

## Identidade visual

| Papel | Cor | Uso |
|---|---|---|
| Marinho noturno | `#0B2239` | Estrutura: menu lateral, áreas institucionais |
| Marinho WayPoint | `#11416C` | Cor do ônibus no logo; placas com o código das linhas |
| Azul sinal | `#1765C1` | Tudo o que é clicável: botões principais, links, foco |
| Verde parada | `#2A8C4E` | O pin do logo: "você está aqui", início da rota, situação normal |
| Âmbar / vermelho | `#B36A00` / `#C9353D` | Somente estados: atraso, lotação, interrupção, prioridade |
| Neutros | `#EEF2F6` `#D9E1E9` `#5A6B7D` `#0F2438` | Fundo, bordas, texto secundário, texto |

- **Tipografia:** Inter e Inter Display (SIL OFL 1.1), servidas localmente em `static/css/fonts/`. A apresentação funciona mesmo sem internet. Horários e contagens usam números tabulares, e os códigos de linha usam o conjunto de desambiguação da fonte (I/l/1, 0/O).
- **Motivo visual:** a linha de trajeto (ponto e traço) aparece apenas onde existe sequência real: a lista de paradas como mapa-esquemático, a faixa de progresso da programação e o marcador da página atual no menu.
- **Contraste:** pares de texto e fundo verificados no nível WCAG AA.

## O que mudou por tela

- **Shell:** menu lateral em todas as telas internas. Em tablet na horizontal ele vira um trilho só com ícones; em tablet na vertical e no celular, uma gaveta com botão de menu, fechamento por Esc e por toque fora. Os destinos são os mesmos de antes (Início, Linhas, Paradas, Reportes, Notificações e, para ADMIN, Painel Admin).
- **Início:** quadro com a situação das linhas, avisos não lidos e reportes recentes. Os dados vêm dos mesmos endpoints GET já usados pelas outras telas (`js/modules/inicio.js`, somente leitura).
- **Paradas:** o mapa é o centro da tela e fica fixo enquanto a coluna da rota rola. A coluna mostra a programação, a parada selecionada e as paradas como mapa-esquemático. O mapa ganhou legenda, e as cores dos pinos seguem a identidade.
- **Linhas, Reportes, Ocorrências e Notificações:** cartões com estado visual. A cor da faixa vem do tipo do reporte ou da prioridade da ocorrência, e notificações não lidas ficam destacadas.
- **Login e cadastro:** layout dividido com ícones nos campos e botão "Mostrar senha". A dica de senha segue a política real (8 caracteres, sem senhas comuns).
- **Painel administrativo:** indicadores com ícones, abas fixas no topo e tabelas e formulários padronizados.

## Arquivos

- Novos: `static/js/ui/shell.js`, `static/js/modules/inicio.js`, `static/img/icons.svg`, `static/img/LogoWaypoint-claro.png`, `static/img/auth-rota.svg`, `static/css/fonts/*`.
- Alterados: todos os templates, `static/css/waypoint.css` e ajustes pontuais de apresentação em `linhas.js`, `reportes.js`, `mapa.js`, `paradas.js` e `painel-testes.js` (textos exibidos, atributo `data-tipo`, cores, formatação de data no admin).
- Backup dos originais: `backup-frontend-original/` (mesma estrutura de pastas). Para reverter, copie os arquivos de volta.

## Observações

- O sistema tem os perfis `ADMIN` e `USER`. Não existe perfil de motorista no código, por isso não há painel de motorista.
- A tela `/ocorrencias` continua sem link no menu, porque os testes (`CadastroLoginControllerTests`) exigem que Início, Linhas e Paradas não tenham `href="/ocorrencias"`.
- O CSS é carregado como `waypoint.css?v=2` para que os navegadores não mantenham a versão antiga em cache.
- Rode `./mvnw test` para confirmar a suíte completa no seu ambiente.
