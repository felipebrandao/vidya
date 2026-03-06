-- V3: Tabela de clientes (espelho local + integração Sankhya)

CREATE TABLE IF NOT EXISTS clientes (
    id              BIGSERIAL       PRIMARY KEY,
    nome            VARCHAR(255)    NOT NULL,
    cgc_cpf         VARCHAR(20),
    nomeparc        VARCHAR(255)    NOT NULL,
    razao_social    VARCHAR(255),
    codcid          INTEGER         NOT NULL,
    tip_pessoa      VARCHAR(1)      NOT NULL CHECK (tip_pessoa IN ('F', 'J')),
    classific_ms    VARCHAR(10),
    cod_sankhya     INTEGER,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clientes_cgc_cpf     ON clientes (cgc_cpf);
CREATE INDEX IF NOT EXISTS idx_clientes_cod_sankhya ON clientes (cod_sankhya);
CREATE INDEX IF NOT EXISTS idx_clientes_codcid      ON clientes (codcid);

