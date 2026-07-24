-- Schema de teste (MySQL — mesma tabela `feedback` alimentada pela func-feedback)
CREATE TABLE feedback (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    description VARCHAR(255),
    rating      INT,
    createdAt   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
