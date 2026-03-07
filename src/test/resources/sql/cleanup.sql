-- Remove apenas registros criados pelos testes, preservando o seed (user-seed)
DELETE FROM clientes WHERE cod_sankhya != 1001;
DELETE FROM cidades   WHERE codcid      != 1234;
DELETE FROM users     WHERE username    != 'user-seed';

