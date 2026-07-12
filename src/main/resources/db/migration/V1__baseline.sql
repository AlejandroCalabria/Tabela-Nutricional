-- V1__baseline.sql
--
-- Esta migration NAO cria nem altera nenhuma tabela.
-- Ela existe apenas para que o Flyway tenha uma V1 valida ao rodar pela
-- primeira vez num banco que ja existe (criado anteriormente pelo Hibernate
-- com ddl-auto=update).
--
-- spring.flyway.baseline-on-migrate=true (em application.properties) faz o
-- Flyway marcar o schema atual como "ja na versao 1", sem tentar recriar
-- nada que ja existe.
--
-- A partir de agora, QUALQUER alteracao de schema (nova coluna, nova tabela,
-- novo indice) deve vir em um novo arquivo:
--   V2__descricao_da_mudanca.sql
--   V3__outra_mudanca.sql
-- nunca editando este arquivo ou alterando entidades e deixando o Hibernate
-- "adivinhar" o schema (ddl-auto agora esta como validate, nao update).

SELECT 1;
