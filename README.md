# WayPoint — Sistema Colaborativo de Transporte Público

## Sobre o projeto

O **WayPoint** é um sistema web desenvolvido como Trabalho de Conclusão de Curso (TCC), com o objetivo de facilitar o acompanhamento do transporte público por meio da centralização de informações sobre linhas, paradas, horários, ocorrências oficiais, comunicados administrativos e reportes colaborativos dos usuários.

A plataforma permite que qualquer pessoa consulte informações públicas do transporte sem precisar criar conta (modo visitante), enquanto usuários autenticados podem favoritar linhas, controlar notificações lidas e enviar ou confirmar reportes nas paradas. Administradores gerenciam dados do sistema, ocorrências, comunicados oficiais e novas contas administrativas.

## Funcionalidades

### 1. Página inicial reformulada e acesso público (Visitante)

- Acesso livre sem login às consultas públicas: página inicial (`/home`), linhas (`/linhas`), paradas e mapa (`/paradas`), feed colaborativo (`/reportes`), ocorrências ativas (`/ocorrencias`) e comunicados oficiais (`/notificacoes`).
- Página inicial moderna com painel de indicadores em tempo real, busca rápida de linhas, filtros (`Todas`, `Com atenção`, `Favoritas`), destaque de comunicados recentes da administração e atalhos rápidos.
- Separação clara entre ações públicas e ações que exigem autenticação (favoritar linhas, registrar/confirmar reportes, controle individual de leitura e painel administrativo).
- Opção "Continuar sem login" nas telas de login e cadastro.

### 2. Linhas e paradas

- Consulta de linhas de transporte público e seu status operacional em tempo real.
- Visualização das paradas associadas a cada linha, organizadas pela ordem oficial e por sentido (`IDA` / `VOLTA`).
- Consulta aos horários cadastrados e previsão de programação da circular.
- Integração com a Google Maps JavaScript API para visualização geográfica da rota.

### 3. Reportes colaborativos

- Registro de reportes relacionados às paradas do transporte público (restrito a usuários autenticados).
- Consulta pública ao feed de reportes recentes (últimos 20 minutos).
- Confirmação de reportes enviados por outros usuários, com incremento de confiabilidade a cada bloco de 5 confirmações.
- Controle de cooldown entre reportes na mesma parada e limpeza automática de registros expirados.

### 4. Ocorrências oficiais

- Cadastro, atualização, encerramento e exclusão de ocorrências por administradores.
- Recálculo automático do status da linha conforme ocorrências ativas.
- Consulta pública às ocorrências ativas; histórico de encerradas restrito ao perfil `ADMIN`.
- Integração automática com o sistema de avisos oficiais e notificações.

**Observação:** os reportes colaborativos são contribuições realizadas pelos usuários, enquanto as ocorrências oficiais são registros administrados pela plataforma. Os dois recursos possuem finalidades distintas.

### 5. Notificações, comunicados oficiais e favoritos

- Arquitetura baseada na entidade canônica `AvisoOficial` vinculada às entregas individuais `Notificacao` e ao controle individual de histórico descartado (`AvisoDescartadoUsuario`).
- Comunicados gerais da administração (como avisos de feriado, manutenção e circulares) permanecem disponíveis tanto para usuários já cadastrados quanto para **novos usuários que se cadastrarem posteriormente**, incluindo histórico e indicadores de não lidas.
- Usuários que favoritarem uma linha posteriormente passam a receber automaticamente os avisos ativos dessa linha.
- Indicadores sincronizados automaticamente entre a página inicial, a barra lateral e a página de notificações.
- Consulta pública de comunicados oficiais em `GET /api/notificacoes/publicas` para visitantes.
- Marcação de notificações como lidas, limpeza individual do histórico superado e **exclusão definitiva de avisos oficiais** pelo painel administrativo (com confirmação e integridade referencial).

### 6. Autenticação, cadastro e segurança

- Cadastro de passageiros com **formatação automática de CPF** (`000.000.000-00`), persistindo apenas os 11 dígitos normalizados.
- Controle de acesso baseado nos perfis **Visitante (anônimo)**, `USER` e `ADMIN` com Spring Security e proteção CSRF.
- Cadastro seguro de novos administradores diretamente pelo painel administrativo (`POST /api/usuarios/admins`), exclusivo para contas `ADMIN` autenticadas.
- Política de senhas (mínimo de 8 caracteres, bloqueio de senhas comuns), criptografia BCrypt e proteção contra tentativas excessivas de login.

### 7. Painel administrativo

- Indicadores em tempo real de linhas, paradas, ocorrências ativas, reportes recentes, notificações e usuários.
- Carregamento automático dos feeds ao alternar entre abas (`Linhas`, `Paradas`, `Ocorrências`, `Reportes`, `Notificações` e `Usuários`).
- Gerenciamento completo de avisos oficiais (envio geral ou por linha, marcação como superado e exclusão definitiva).
- Gerenciamento de usuários e criação de contas `ADMIN`.

## Tecnologias utilizadas

| Tecnologia | Finalidade |
|---|---|
| Java 21 | Linguagem de programação |
| Spring Boot 3.5.13 | Estrutura principal do back-end |
| Spring Web | Desenvolvimento de controladores e endpoints HTTP |
| Spring Security | Autenticação, autorização e proteção CSRF |
| Spring Data JPA | Persistência e acesso aos dados |
| Hibernate | Mapeamento objeto-relacional (ORM) |
| PostgreSQL | Banco de dados principal |
| Thymeleaf | Renderização das páginas web |
| HTML5 e CSS3 | Estrutura e apresentação da interface |
| JavaScript | Interatividade, máscara de CPF e integração com a API |
| Google Maps JavaScript API | Recursos de mapas |
| Springdoc OpenAPI / Swagger UI | Documentação da API |
| Maven | Gerenciamento de dependências e build |
| JUnit e Spring Boot Test | Testes automatizados e de integração |
| H2 | Banco de dados utilizado no ambiente de testes |

## Arquitetura do projeto

O projeto utiliza uma arquitetura em camadas, com separação de responsabilidades para facilitar a organização, a manutenção e a evolução da aplicação.

```text
WayPoint/
├── database/
│   └── Scripts e arquivos auxiliares do banco
├── project-tcc-app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/waypoint/project_tcc_app/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── exception/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   ├── security/
│   │   │   │   └── service/
│   │   │   └── resources/
│   │   │       ├── static/
│   │   │       │   ├── css/
│   │   │       │   ├── img/
│   │   │       │   └── js/
│   │   │       ├── templates/
│   │   │       └── application.properties
│   │   └── test/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
├── CONFIGURACAO_LOCAL.md
├── REDESIGN.md
└── README.md
```

### Responsabilidades das camadas

- **Config:** configuração OpenAPI e migração idempotente de avisos legados na inicialização (`NotificacaoLegadoMigracaoRunner`).
- **Controller:** recebe requisições HTTP das páginas Thymeleaf e dos endpoints REST.
- **Service:** concentra as regras de negócio, sincronização de notificações e validações.
- **Repository:** realiza operações de persistência por meio do Spring Data JPA.
- **Model:** representa as entidades de domínio (`Linha`, `Parada`, `HorarioParada`, `Ocorrencia`, `ReporteUsuario`, `ConfirmacaoReporte`, `AvisoOficial`, `AvisoDescartadoUsuario`, `Notificacao`, `User`, `UsuarioLinhaFavorita`).
- **DTO:** define contratos de entrada e saída da API sem expor dados sensíveis.
- **Security:** implementa autenticação, autorização por perfil, política de senhas e proteção contra força bruta.
- **Resources:** contém templates HTML, CSS, JavaScript modular e configurações por ambiente.

## Requisitos para execução

Antes de executar o projeto, verifique se os seguintes componentes estão instalados:

- Java Development Kit (JDK) 21.
- PostgreSQL.
- Git.
- Uma IDE compatível com Java, como IntelliJ IDEA, ou um terminal.
- Acesso à internet para baixar as dependências Maven na primeira execução.

Para utilizar os recursos de mapas, também é necessária uma chave válida da Google Maps JavaScript API, com as APIs e permissões correspondentes configuradas no Google Cloud.

## Como executar o projeto

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd WayPoint
```

Substitua `<URL_DO_REPOSITORIO>` pelo endereço real do repositório.

### 2. Criar o banco de dados

No PostgreSQL, crie o banco utilizado pela aplicação:

```sql
CREATE DATABASE waypointbd;
```

A configuração padrão utiliza os seguintes parâmetros:

| Parâmetro | Valor padrão |
|---|---|
| Host | `localhost` |
| Porta | `5432` |
| Banco de dados | `waypointbd` |
| Usuário | `postgres` |

O usuário configurado precisa possuir as permissões necessárias para a aplicação.

No perfil de desenvolvimento (`dev`), o Hibernate utiliza `spring.jpa.hibernate.ddl-auto=update`, criando e atualizando automaticamente as tabelas (incluindo `aviso_oficial` e `aviso_descartado_usuario`). Na inicialização, notificações legadas existentes na tabela `notificacao` são vinculadas automaticamente a registros canônicos em `aviso_oficial`.

### 3. Configurar as variáveis de ambiente

Configure as credenciais do PostgreSQL no ambiente em que a aplicação será executada:

```properties
DB_URL=jdbc:postgresql://localhost:5432/waypointbd
DB_USERNAME=postgres
DB_PASSWORD=sua_senha_local
```

Para habilitar a integração com o Google Maps, configure também:

```properties
GOOGLE_MAPS_API_KEY=sua_chave_local
GOOGLE_MAPS_MAP_ID=seu_map_id
```

A chave do Google Maps é opcional para iniciar a aplicação; sem ela, todas as demais telas funcionam normalmente e o mapa exibe um aviso amigável.

**Importante:** não publique senhas, tokens ou chaves privadas no GitHub. Utilize variáveis de ambiente ou arquivos de configuração locais ignorados no `.gitignore`.

### 4. Iniciar a aplicação

No Windows (PowerShell), defina o JDK 21 caso ele não seja o padrão do sistema e execute:

```powershell
cd project-tcc-app
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS, execute:

```bash
cd project-tcc-app
chmod +x mvnw
./mvnw spring-boot:run
```

Após a inicialização, acesse:

[http://localhost:8080](http://localhost:8080)

A rota raiz redireciona diretamente para `/home` em modo visitante, permitindo consulta pública imediata e acesso às páginas `/login` e `/cadastro`.

## API e documentação

O back-end disponibiliza endpoints REST organizados por recurso e protegidos por perfil:

| Rota base | Acesso principal |
|---|---|
| `GET /api/linhas`, `GET /api/linhas/{id}`, `GET /api/linhas/{id}/paradas` | Público (Visitante, `USER`, `ADMIN`) |
| `GET /api/paradas`, `GET /api/paradas/{id}`, `GET /api/paradas/{id}/horarios` | Público (Visitante, `USER`, `ADMIN`) |
| `GET /api/ocorrencias`, `GET /api/linhas/{id}/ocorrencias` | Público para ativas; `ADMIN` vê todas |
| `GET /api/reportes/feed` | Público (somente leitura) |
| `GET /api/notificacoes/publicas` | Público (comunicados oficiais) |
| `POST /api/reportes/**`, `/api/usuarios/*/linhas-favoritas/**`, `GET /api/usuarios/*/notificacoes`, `PATCH /api/notificacoes/*/lida`, `DELETE /api/notificacoes/minhas/historico` | Autenticado (`USER`, `ADMIN`) |
| `POST/PUT/PATCH/DELETE` em linhas, paradas, ocorrências, avisos oficiais (`DELETE /api/notificacoes/avisos/{avisoId}`), `GET /api/notificacoes/avisos`, `GET /api/usuarios`, `POST /api/usuarios/admins` | Exclusivo `ADMIN` |

A documentação OpenAPI está configurada para a API WayPoint, versão `v1` (restrita ao perfil `ADMIN`):

- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Especificação OpenAPI:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Executar os testes

Para executar a suíte completa de testes automatizados (73 testes de integração e segurança com MockMvc e H2), utilize o Maven Wrapper:

**Windows:**

```powershell
cd project-tcc-app
.\mvnw.cmd test
```

**Linux ou macOS:**

```bash
cd project-tcc-app
./mvnw test
```

Os testes cobrem inicialização da aplicação, cadastro (inclusive CPF formatado), login, bloqueio por tentativas, acesso visitante vs autenticado vs `ADMIN`, fluxos de linhas, paradas, horários, reportes, ocorrências, sincronização de notificações para novos usuários, exclusão administrativa de avisos e criação segura de administradores.

## Configurações por ambiente

O projeto possui arquivos de configuração destinados a diferentes ambientes:

- `application.properties`: configurações gerais e conexão padrão com o PostgreSQL.
- `application-dev.properties`: configurações voltadas ao desenvolvimento (`ddl-auto=update`).
- `application-prod.properties`: configurações voltadas à produção (`ddl-auto=validate`, cookie seguro).

O perfil padrão definido no projeto é `dev`. Para obter orientações adicionais sobre a configuração local, consulte o arquivo [`CONFIGURACAO_LOCAL.md`](CONFIGURACAO_LOCAL.md).

## Objetivo acadêmico

O WayPoint é desenvolvido como Trabalho de Conclusão de Curso (TCC), explorando conceitos de desenvolvimento web, APIs REST, persistência de dados, segurança de aplicações, integração com serviços externos e arquitetura de software em camadas.

## Equipe de desenvolvimento

- **Felipe Cavalcante da Costa** — Back-end.
- **Otávio Franco Rodrigues** — Banco de dados.
- **Leonardo Juliano Hernandes Trovo** — Front-end.

---

Desenvolvido como Trabalho de Conclusão de Curso (TCC).
