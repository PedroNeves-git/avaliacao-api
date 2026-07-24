# Fluxo Function Week Report 

```mermaid
graph LR
    %% Definição dos nós
    Timer((⏱️ Timer CRON))
    Func(⚡ Function)
    DB[(🛢️ Banco MySQL)]
    Blob[(💾 Blob Storage)]

    %% Conexões do Fluxo
    Timer --> Func
    Func <--> DB
    Func --> Blob

    %% Definição das classes de estilo usando palavras-chave de cores para evitar erro de parser
    classDef timerStyle fill:#5fd4f4,stroke:white,stroke-width:2px,color:black;
    classDef funcStyle fill:#f39c12,stroke:white,stroke-width:2px,color:white;
    classDef dbStyle fill:#2980b9,stroke:white,stroke-width:2px,color:white;
    classDef blobStyle fill:#2ecc71,stroke:white,stroke-width:2px,color:white;

    %% Aplicação das classes aos nós
    class Timer timerStyle;
    class Func funcStyle;
    class DB dbStyle;
    class Blob blobStyle;
    
    %% Estilo das setas
    linkStyle default stroke:gray,stroke-width:2px;
