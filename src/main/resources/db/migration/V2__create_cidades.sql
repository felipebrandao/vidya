-- V2: Tabela de cidades (espelho local da API Sankhya)

CREATE TABLE IF NOT EXISTS cidades (
    id          BIGSERIAL       PRIMARY KEY,
    codcid      INTEGER         NOT NULL UNIQUE,
    nome        VARCHAR(150)    NOT NULL,
    uf          VARCHAR(2),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cidades_codcid ON cidades (codcid);
CREATE INDEX IF NOT EXISTS idx_cidades_uf     ON cidades (uf);

