# Fluxo Function Notification

```mermaid
graph LR
    %% Definição dos nós
    Fila[📥 Fila]
    Func(⚡ Function)
    ACS[✉️ Azure Communication Service]

    %% Conexões do Fluxo
    Fila --> Func
    Func --> ACS

    %% Definição das classes de estilo usando cores compatíveis
    classDef filaStyle fill:#8e44ad,stroke:white,stroke-width:2px,color:white;
    classDef funcStyle fill:#f39c12,stroke:white,stroke-width:2px,color:white;
    classDef acsStyle fill:#34495e,stroke:white,stroke-width:2px,color:white;

    %% Aplicação das classes aos nós
    class Fila filaStyle;
    class Func funcStyle;
    class ACS acsStyle;
    
    %% Estilo das setas
    linkStyle default stroke:gray,stroke-width:2px;
