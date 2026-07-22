-- Schema de teste (MySQL — mesmo formato de db/schema.sql da raiz do repositório)
CREATE TABLE avaliacoes (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    descricao   VARCHAR(1000) NOT NULL,
    nota        INT           NOT NULL,
    urgencia    VARCHAR(10)   NOT NULL,
    data_envio  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
);