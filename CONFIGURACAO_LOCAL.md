# Configuracao local do WayPoint

Este projeto nao versiona chaves reais, tokens ou senhas privadas. Cada integrante deve configurar as variaveis necessarias em sua propria maquina.

## Banco

Configuracao padrao atual:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/waypointbd}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:}
```

## PostgreSQL

O WayPoint utiliza PostgreSQL como banco principal da aplicacao.

Configuracao local esperada:

```text
Host: localhost
Porta: 5432
Database: waypointbd
Usuario padrao: postgres
```

Variaveis de ambiente recomendadas:

```text
DB_URL=jdbc:postgresql://localhost:5432/waypointbd
DB_USERNAME=postgres
DB_PASSWORD=<senha local>
```

Nao coloque a senha real em `application.properties`, no GitHub ou em arquivos versionados.

O banco `waypointbd` deve existir antes de iniciar a aplicacao. O Hibernate esta configurado com `spring.jpa.hibernate.ddl-auto=update`, entao ele cria/atualiza as tabelas dentro desse banco conforme as entidades JPA atuais.

Os scripts na pasta `database/` sao historicos/legados do MariaDB, exceto quando forem explicitamente revisados para PostgreSQL em uma etapa futura. Nao execute automaticamente `database/schema-waypointbd.sql` no PostgreSQL.

## Google Maps

A chave real do Google Maps nao acompanha o GitHub.

Configure localmente:

```text
GOOGLE_MAPS_API_KEY=SUA_CHAVE_LOCAL
GOOGLE_MAPS_MAP_ID=DEMO_MAP_ID
```

O projeto usa:

```properties
waypoint.google-maps.api-key=${GOOGLE_MAPS_API_KEY:}
waypoint.google-maps.map-id=${GOOGLE_MAPS_MAP_ID:DEMO_MAP_ID}
```

Sem `GOOGLE_MAPS_API_KEY`, a aplicacao deve iniciar normalmente. Apenas a visualizacao do mapa fica indisponivel.

## IntelliJ IDEA

Para configurar a chave do Google Maps e a senha do PostgreSQL na IDE:

1. Abra `Run`.
2. Acesse `Edit Configurations`.
3. Selecione `ProjectTccAppApplication`.
4. Em `Environment variables`, adicione:

```text
DB_URL=jdbc:postgresql://localhost:5432/waypointbd
DB_USERNAME=postgres
DB_PASSWORD=<senha local>
GOOGLE_MAPS_API_KEY=SUA_CHAVE_LOCAL
GOOGLE_MAPS_MAP_ID=DEMO_MAP_ID
```

Se possuir um Map ID proprio, substitua `DEMO_MAP_ID` pelo seu valor local. Nao salve senhas reais em `.idea/workspace.xml`.

## Novo computador

1. Clone o repositorio.
2. Abra o projeto no IntelliJ.
3. Aguarde o Maven carregar as dependencias.
4. Inicie o PostgreSQL.
5. Crie o banco `waypointbd`, caso ainda nao exista.
6. Configure `DB_PASSWORD` localmente.
7. Configure `GOOGLE_MAPS_API_KEY` localmente, se for testar mapas.
8. Execute a aplicacao Spring Boot.

## Importante

Nunca coloque no GitHub:

- chave real do Google Maps;
- token GitHub;
- senha real;
- arquivo `.env` com valores reais;
- chaves privadas;
- certificados privados.
