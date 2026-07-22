-- Schema de teste (T-SQL — mesmo formato de db/schema.sql da raiz do repositório)
CREATE TABLE avaliacoes (
    id          UNIQUEIDENTIFIER  NOT NULL DEFAULT NEWID(),
    descricao   NVARCHAR(1000)    NOT NULL,
    nota        INT               NOT NULL,
    urgencia    VARCHAR(10)       NOT NULL,
    data_envio  DATETIME2         NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_avaliacoes PRIMARY KEY (id)
);
