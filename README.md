# Vidya

Serviço em Java com Spring Boot para cadastro e listagem de clientes no ERP Sankhya, com persistência local em PostgreSQL e autenticação JWT.

## Tecnologias

- Java 17
- Spring Boot 3.5
- Spring Security + JWT
- PostgreSQL
- Flyway
- MapStruct
- OpenFeign
- Docker

## Pré-requisitos

- Java 17+
- Maven ou usar o `./mvnw` incluso
- Docker e Docker Compose

## Como rodar localmente

### 1. Clonar o repositório

```bash
git clone https://github.com/seu-usuario/vidya.git
cd vidya
```

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Edite o `.env` e preencha os valores:

| Variável | Descrição |
|---|---|
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta JWT (256 bits) — gere com `openssl rand -hex 32` |
| `JWT_EXPIRATION` | Expiração do token em ms (padrão: `86400000` = 24h) |
| `SANKHYA_BASE_URL` | URL base do ERP Sankhya |
| `SANKHYA_USERNAME` | Usuário do Sankhya |
| `SANKHYA_PASSWORD` | Senha do Sankhya |

### 3. Subir o banco de dados com Docker

```bash
docker compose up -d
```

Isso cria um container PostgreSQL na porta `5432` com o banco `vidyadb`.

Para verificar se subiu corretamente:

```bash
docker compose ps
```

### 4. Rodar a aplicação

No Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/vidyadb"
$env:DB_USERNAME="vidya"
$env:DB_PASSWORD="vidya"
$env:JWT_SECRET="sua-chave-aqui"
$env:SANKHYA_BASE_URL="sua-url-aqui"
$env:SANKHYA_USERNAME="seu-usuario-aqui"
$env:SANKHYA_PASSWORD="sua-senha-aqui"
.\mvnw spring-boot:run
```

No Linux/macOS:
```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

O Flyway criará as tabelas automaticamente ao iniciar.

### 5. Acessar o Swagger

```
http://localhost:8080/swagger-ui.html
```

## Endpoints

### Autenticação

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cadastrar novo usuário |
| `POST` | `/auth/login` | Login e obter token JWT |

### Clientes (requer Bearer Token)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/clients` | Listar clientes (sincroniza com Sankhya) |
| `POST` | `/clients` | Cadastrar cliente (local + Sankhya) |
| `GET` | `/clients/cnpj/{cnpj}` | Consultar dados do CNPJ via ReceitaWS |

### Cidades (requer Bearer Token)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/cities` | Listar cidades (sincroniza com Sankhya) |

## Parar o banco

```bash
docker compose down
```

Para remover os dados também:

```bash
docker compose down -v
```


