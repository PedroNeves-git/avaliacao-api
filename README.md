# 📣 Plataforma de Feedback Serverless — Tech Challenge Fase 4

<div align="center">
 <h2> Sumário</h2>
  <a href="#descrição-do-projeto">Descrição do projeto</a> -
  <a href="#modelo-de-cloud">Modelo de cloud</a> -
  <a href="#arquitetura">Arquitetura</a> -
  <a href="#as-funções-serverless">As funções serverless</a> -
  <a href="#fluxo-de-funcionamento">Fluxo de funcionamento</a> -
  <a href="#ferramentas-utilizadas">Ferramentas utilizadas</a> -
  <a href="#guia-de-implantação">Guia de implantação</a> -
  <a href="#deploy-automatizado-cicd">Deploy automatizado (CI/CD)</a> -
  <a href="#monitoramento">Monitoramento</a> -
  <a href="#segurança-e-governança">Segurança e governança</a> -
  <a href="#recursos-do-azure">Recursos do Azure</a> -
  <a href="#desenvolvedores">Desenvolvedores</a>
</div>

## Descrição do projeto

<p align="justify">
Este projeto foi criado para a quarta fase do Tech Challenge da pós-graduação em Desenvolvimento e Arquitetura Java da instituição FIAP.

A aplicação é uma <b>plataforma de feedback serverless</b>: estudantes avaliam aulas e a solução automatiza o recebimento das avaliações, a <b>notificação de administradores</b> quando um feedback é crítico e a geração de um <b>relatório semanal</b> consolidado. Tudo roda em <b>Azure Functions</b> (serverless), com serviços gerenciados da nuvem para mensageria, banco de dados, e-mail e monitoramento.

O repositório é um <b>monorepo</b> composto por três funções independentes, cada uma com responsabilidade única e seu próprio ciclo de build/deploy. A comunicação entre a função de recebimento e a de notificação é <b>desacoplada por eventos</b> (fila), seguindo um padrão orientado a mensagens. A função de notificação foi construída em <b>Clean Architecture</b> (domínio / caso de uso / gateway / infraestrutura).
</p>

## Modelo de cloud

O modelo escolhido é **FaaS (Function as a Service)** na **Azure**, com plano **Consumption** (serverless):

- **Serverless / pay-per-use:** as funções só consomem recursos quando são acionadas — ideal para um ambiente de créditos limitados.
- **Orientado a eventos:** cada função tem um gatilho próprio (HTTP, fila, timer), sem servidores para gerenciar.
- **Serviços gerenciados:** banco (Azure Database for MySQL), mensageria (Azure Storage Queue), e-mail (Azure Communication Services), armazenamento de relatórios (Azure Blob Storage) e observabilidade (Application Insights).
- **Responsabilidade única:** cada função faz uma coisa só, o que facilita evolução, deploy e testes de forma independente.

## Arquitetura

O sistema é composto por três funções serverless que se integram por fila (assíncrono) e por banco de dados compartilhado.

```
                          POST /api/feedback
   [Estudante]  ───────────────────────────────►  [fn-feedback]
                                                       │  grava
                                                       ▼
                            (lê semanalmente)      [ MySQL ]
   [fn-week-report] ◄───────────────────────────────  │
        │  gera                                        │  se nota < 6, enfileira
        ▼                                              ▼
  [ Blob Storage ]                       [ Storage Queue: feedback-critico ]
  (relatorio .json)                                    │  gatilho
                                                       ▼
                                               [fn-notification]
                                                       │  se CRÍTICO (nota 0–3)
                                                       ▼
                                    [ Azure Communication Services ]
                                                       │
                                                       ▼
                                             e-mail para o administrador
```

| Função | Gatilho | Responsabilidade | Function App (Azure) |
|---|---|---|---|
| **fn-feedback** | HTTP `POST /api/feedback` | Recebe o feedback, valida, grava no MySQL e, se a nota for baixa, publica na fila. | `func-feedback-tech-challenge` |
| **fn-notification** | Storage Queue (`feedback-critico`) | Detecta feedbacks críticos e envia e-mail de alerta aos administradores via ACS. | `func-notification-tech-challenge` |
| **fn-week-report** | Timer (semanal) | Consolida os feedbacks da semana, calcula a média e publica o relatório no Blob Storage. | `fn-week-report` |

## As funções serverless

### 1. `fn-feedback` — Receber Feedback
- **Gatilho:** HTTP `POST /api/feedback` com o corpo `{ "description": "...", "rating": 0-10 }`.
- **Fluxo:** persiste o feedback no MySQL e, quando a nota indica baixa satisfação, publica o feedback (em JSON) na fila `feedback-critico` para processamento assíncrono.
- **Stack:** Quarkus (Azure Functions HTTP) · Hibernate/Panache · MySQL · SDK `azure-storage-queue`.

### 2. `fn-notification` — Notificar Crítico
- **Gatilho:** mensagem na fila `feedback-critico` (Storage Queue).
- **Fluxo:** lê o feedback diretamente da mensagem (**não acessa banco**), classifica a urgência pela nota e, se for **crítico**, envia um e-mail aos administradores com descrição, urgência e data de envio.
- **Regra de urgência (no domínio):** `0–3 = CRÍTICO` · `4–6 = MODERADO` · `7–10 = NORMAL`. Só a faixa **crítica** dispara e-mail.
- **Arquitetura (Clean Architecture):**

  ```
  com.fiap.notification
  ├── domain/          Feedback.java · Urgency.java        (regra de negócio)
  ├── gateway/         EmailGateway.java · AlertMessage.java (portas)
  ├── usecase/         NotifyAdminUseCase.java              (orquestração)
  └── infrastructure/
      ├── email/       AcsEmailGateway.java                 (Azure Communication Services)
      └── function/    CriticalFeedbackFunction.java        (gatilho da fila) · FeedbackEvent.java
  ```
- **Stack:** Quarkus (Azure Functions Queue Trigger) · Azure Communication Services (`azure-communication-email`) · Jackson.

### 3. `fn-week-report` — Relatório Semanal
- **Gatilho:** Timer (agendado, semanal).
- **Fluxo:** consulta os feedbacks da última semana, calcula a média das notas e as contagens por dia e por urgência, e publica o relatório consolidado (JSON) no Azure Blob Storage.
- **Stack:** Quarkus (Azure Functions Timer Trigger) · JDBC · MySQL · SDK `azure-storage-blob`.

## Fluxo de funcionamento

1. O estudante envia um feedback em `POST /api/feedback` (**fn-feedback**).
2. A **fn-feedback** grava no MySQL e, se a nota for baixa, coloca a mensagem na fila `feedback-critico`.
3. O runtime do Azure detecta a mensagem e aciona a **fn-notification**, entregando o conteúdo pronto.
4. A **fn-notification** classifica a urgência; se for crítico, envia o e-mail via **Azure Communication Services**.
5. Semanalmente, a **fn-week-report** consolida os feedbacks e publica o relatório no **Blob Storage**.

## Ferramentas utilizadas
<div style="display: flex; gap: 15px">
<a href="https://www.java.com" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" alt="Java" width="40" height="40"/>
</a>
<a href="https://quarkus.io/" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/quarkus/quarkus-original.svg" alt="Quarkus" width="40" height="40"/>
</a>
<a href="https://azure.microsoft.com/" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/azure/azure-original.svg" alt="Azure" width="40" height="40"/>
</a>
<a href="https://www.mysql.com/" target="_blank">
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/mysql/mysql-original.svg" alt="MySQL" width="40" height="40"/>
</a>
<a href="https://github.com/features/actions" target="_blank">
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/githubactions/githubactions-original.svg" alt="GitHub Actions" width="40" height="40"/>
</a>
<a href="https://maven.apache.org/" target="_blank">
    <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/apache/apache-original.svg" alt="Maven" width="40" height="40"/>
</a>
</div>

**Stack detalhada:** Java 17 · Quarkus 3.37 · Azure Functions (runtime v4, plano Consumption/Linux) · Azure Database for MySQL · Azure Storage Queue · Azure Communication Services (e-mail) · Azure Blob Storage · Application Insights · Maven · GitHub Actions.

## Guia de implantação

### Pré-requisitos
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli) (`az login`)
- Java 17 e Maven
- [Git](https://git-scm.com/)
- Uma assinatura Azure com um Resource Group provisionado

### Clonar o repositório
```bash
git clone https://github.com/PedroNeves-git/avaliacao-api.git
cd avaliacao-api
```

### Deploy manual (por função)
Cada função é uma app Quarkus independente. A partir da pasta da função:
```bash
cd fn-notification   # ou fn-feedback / fn-week-report
mvn -Dquarkus.profile=prod \
    -Dquarkus.azure-functions.resource-group=<seu-RG> \
    -Dquarkus.azure-functions.region=<sua-regiao> \
    clean package quarkus:deploy
```

### Configuração (App Settings)
As credenciais **não ficam no código** — são configuradas como *app settings* (variáveis de ambiente) em cada Function App:

| Função | Variáveis |
|---|---|
| `fn-feedback` | `QUARKUS_DATASOURCE_JDBC_URL` · `QUARKUS_DATASOURCE_USERNAME` · `QUARKUS_DATASOURCE_PASSWORD` · `AZURE_STORAGE_CONNECTION_STRING` · `AZURE_STORAGE_QUEUE_NAME` |
| `fn-notification` | `QUEUE_NAME` · `QUEUE_CONNECTION` · `ACS_CONNECTION_STRING` · `ACS_SENDER_ADDRESS` · `ADMIN_EMAILS` |
| `fn-week-report` | credenciais do MySQL · connection string do Blob Storage |

> Na `fn-notification`, o nome e a conexão da fila são configuráveis (`QUEUE_NAME` / `QUEUE_CONNECTION`), então dá para trocar de fila/storage **sem alterar o código**.

## Deploy automatizado (CI/CD)

O deploy é automatizado com **GitHub Actions** (`.github/workflows/deploy.yml`):

- **Gatilho:** todo `push`/merge no branch `main` (e disparo manual via `workflow_dispatch`).
- **Passos:** para cada função — checkout → build com Maven → deploy no Azure via `Azure/functions-action`.
- **Autenticação:** cada Function App tem seu **publish profile** guardado como *secret* no GitHub (`PUBLISH_PROFILE_*`), sem credenciais no código.

Disparo manual (sem commit): aba **Actions** → **Deploy Azure Functions** → **Run workflow**.

## Monitoramento

Cada Function App está conectado ao **Azure Application Insights**, que captura logs, execuções e falhas. Onde acompanhar:

| O que ver | Onde (Portal) |
|---|---|
| Logs ao vivo | Function App → **Log stream** |
| Logs por consulta | Application Insights → **Logs** → `traces` |
| Histórico de execuções | Função → **Invocations** / **Monitor** |

Exemplo de consulta (KQL) no Application Insights:
```
traces | where timestamp > ago(30m) | project timestamp, message | order by timestamp desc
```

## Segurança e governança

- **Segredos fora do código:** connection strings e chaves ficam em *app settings* (Azure) e *GitHub Secrets* (CI/CD), nunca versionados.
- **Governança de acesso:** acesso à assinatura por RBAC (papel *Contributor*/*Owner* por Resource Group).
- **Comunicação segura:** MySQL com `sslMode=REQUIRED` e firewall; ACS autenticado por connection string.
- **Deploy autenticado:** publish profiles com SCM basic auth, isolados por função (menor privilégio).

## Recursos do Azure

| Recurso | Nome | Papel |
|---|---|---|
| Resource Group | `rg-tech-challenge-4` | Agrupa todos os recursos |
| Storage Account | `rgtechchallenge4bb0a` | Hospeda a fila `feedback-critico` |
| Azure Database for MySQL | `mysql-tech-challenge-4-br` | Banco de feedbacks |
| Communication Services | `acs-avaliacao` (+ `acs-email-avaliacao`) | Envio de e-mail |
| Function App — Receber | `func-feedback-tech-challenge` | fn-feedback |
| Function App — Notificar | `func-notification-tech-challenge` | fn-notification |
| Function App — Relatório | `fn-week-report` | fn-week-report |
| Application Insights | (um por função) | Monitoramento |

## Desenvolvedores
<table align="center">
  <tr>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/PedroNeves-git" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Pedro Neves   </b><br>
            <a href="https://www.linkedin.com/in/pedro-neves-867001258/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/PedroNeves-git" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/breenoox" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Breno Barbosa   </b><br>
            <a href="https://www.linkedin.com/in/brenobarbosa22/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/breenoox" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
    <td align="center">
      <div>
        <img src="https://avatars.githubusercontent.com/GuiFonsCode" width="120px;" alt="Foto no GitHub" class="profile"/><br>
          <b> Guilherme Fonseca   </b><br>
            <a href="https://www.linkedin.com/in/guifonseca1212/" alt="Linkedin"><img src="https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" height="20"></a>
            <a href="https://github.com/GuiFonsCode" alt="Github"><img src="https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white" height="20"></a>
      </div>
    </td>
  </tr>
</table>
