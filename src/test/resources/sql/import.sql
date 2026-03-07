INSERT INTO users (username, password, created_at)
VALUES ('user-seed', '$2a$10$W0DhYRxlhBUF7M.7q/UbNOCzJ465px9wTDYKy0zKubNbzCbZMKwzK', CURRENT_TIMESTAMP);

INSERT INTO cidades (codcid, nome, uf, updated_at)
VALUES (1234, 'São Paulo', 'SP', CURRENT_TIMESTAMP);

INSERT INTO clientes (nome, cgc_cpf, nomeparc, razao_social, codcid, tip_pessoa, classific_ms, cod_sankhya, created_at, updated_at)
VALUES ('Empresa Teste Ltda', '12.345.678/0001-99', 'Empresa Teste', 'Empresa Teste Razão Social', 1234, 'J', 'A1', 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


