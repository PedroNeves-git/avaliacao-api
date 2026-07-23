#!/usr/bin/env bash
# ============================================================================
# Provisionamento Azure — fn-relatorio-semanal (Tech Challenge FIAP, Fase 4)
#
# Cria, na ordem:
#   1. Resource Group
#   2. Conexão com o Azure Database for MySQL (instância já existente —
#      este script não a cria, apenas aplica o schema e configura a função
#      para usá-la)
#   3. Storage Account (AzureWebJobsStorage + container "reports")
#   4. Function App (Java 17, plano Consumption) + App Settings
#
# Banco de dados: usa a instância MySQL já provisionada
# mysql-tech-challenge-4-br.mysql.database.azure.com (database "techchallenge").
# O firewall/rede dessa instância é gerenciado fora deste script — garanta que
# o IP desta máquina e os serviços do Azure tenham acesso liberado antes de
# rodar (regra "Allow public access from any Azure service" ou IP específico).
#
# Pré-requisitos:
#   - Azure CLI instalado e logado (az login)
#   - Senha do banco na variável de ambiente SQL_ADMIN_PASSWORD
#     (se ausente, é pedida interativamente sem eco no terminal)
#   - mysql client (opcional, para aplicar o schema)
#
# Uso:
#   export SQL_ADMIN_PASSWORD='<senha do usuário admin_db>'
#   ./provision.sh
#
# O script é seguro de re-executar: recursos já existentes são reaproveitados.
# Depois dele, o deploy do código é: az login && ./mvnw clean package quarkus:deploy
# ============================================================================
set -euo pipefail

# ── Configuração (mesmos valores do application.properties) ─────────────────
RESOURCE_GROUP="rg-tech-challenge-4"
# eastus: onde já vivem o MySQL e o restante da infra deste RG. O RG está
# marcado como brazilsouth nos metadados, mas os recursos ficam em eastus —
# criar a função na mesma região do banco reduz latência e evita bloqueios
# de política ('RegionDoesNotAllowProvisioning') da assinatura de estudante.
LOCATION="eastus"
FUNCTION_APP="fn-week-report"

# Instância MySQL já existente — não é criada por este script.
MYSQL_HOST="mysql-tech-challenge-4-br.mysql.database.azure.com"
MYSQL_PORT="3306"
DB_NAME="techchallenge"
SQL_ADMIN_USER="admin_db"

# Nome global precisa ser único no Azure inteiro; o sufixo torna isso
# reprodutível por assinatura sem colidir com nomes de outros alunos.
SUFFIX=$(az account show --query id -o tsv | cut -c1-8)
STORAGE_ACCOUNT="sttechchallenge${SUFFIX}"   # só minúsculas/números, máx. 24 chars
BLOB_CONTAINER="reports"

# Senha via variável de ambiente ou, na falta dela, pedida interativamente
# (sem eco no terminal e sem passar por argumento, para não vazar no histórico).
# Aceita MYSQL_ADMIN_PASSWORD como alias.
SQL_ADMIN_PASSWORD="${SQL_ADMIN_PASSWORD:-${MYSQL_ADMIN_PASSWORD:-}}"
if [[ -z "$SQL_ADMIN_PASSWORD" ]]; then
    read -r -s -p "Senha do admin do MySQL (SQL_ADMIN_PASSWORD): " SQL_ADMIN_PASSWORD
    echo
fi
: "${SQL_ADMIN_PASSWORD:?Senha vazia — defina SQL_ADMIN_PASSWORD ou informe quando solicitado}"

echo "==> Assinatura: $(az account show --query name -o tsv)"
echo "==> Recursos: RG=$RESOURCE_GROUP | MySQL=$MYSQL_HOST | Storage=$STORAGE_ACCOUNT | App=$FUNCTION_APP"

# ── 1. Resource Group ────────────────────────────────────────────────────────
echo "==> [1/4] Resource Group"
# O RG deste desafio já existe (região dos metadados = brazilsouth), mas os
# recursos vivem em eastus. NÃO herdamos a região do RG de propósito: os novos
# recursos são criados em $LOCATION (eastus), junto do banco.
if az group show --name "$RESOURCE_GROUP" --output none 2>/dev/null; then
    echo "    - já existe — criando recursos em '$LOCATION' (mesma região do banco)"
else
    az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
fi

# ── 2. Banco de dados (MySQL — instância existente) ──────────────────────────
echo "==> [2/4] Banco de dados MySQL (instância existente: $MYSQL_HOST)"
SCHEMA_FILE="$(cd "$(dirname "$0")/../.." && pwd)/db/schema.sql"
if command -v mysql >/dev/null 2>&1; then
    if [[ -f "$SCHEMA_FILE" ]]; then
        echo "    - aplicando schema ($SCHEMA_FILE — idempotente, usa IF NOT EXISTS)"
        mysql --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$SQL_ADMIN_USER" \
            --password="$SQL_ADMIN_PASSWORD" --ssl-mode=REQUIRED "$DB_NAME" < "$SCHEMA_FILE"
    else
        echo "    - AVISO: schema não encontrado em $SCHEMA_FILE — pulando aplicação"
    fi
else
    echo "    - AVISO: cliente 'mysql' não encontrado — aplique o schema manualmente:"
    echo "        mysql --host=$MYSQL_HOST --user=$SQL_ADMIN_USER -p $DB_NAME < db/schema.sql"
fi

# ── 3. Storage Account ───────────────────────────────────────────────────────
echo "==> [3/4] Storage Account"
az storage account create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$STORAGE_ACCOUNT" \
    --location "$LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --output none

STORAGE_CONNECTION_STRING=$(az storage account show-connection-string \
    --resource-group "$RESOURCE_GROUP" \
    --name "$STORAGE_ACCOUNT" \
    --query connectionString -o tsv)

echo "    - container '$BLOB_CONTAINER' (a função também o cria sozinha, se faltar)"
az storage container create \
    --name "$BLOB_CONTAINER" \
    --connection-string "$STORAGE_CONNECTION_STRING" \
    --output none

# ── 4. Function App + App Settings ───────────────────────────────────────────
echo "==> [4/4] Function App (Java 17, Consumption)"
if ! az functionapp show --resource-group "$RESOURCE_GROUP" --name "$FUNCTION_APP" --output none 2>/dev/null; then
    az functionapp create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$FUNCTION_APP" \
        --consumption-plan-location "$LOCATION" \
        --storage-account "$STORAGE_ACCOUNT" \
        --runtime java \
        --runtime-version 17 \
        --functions-version 4 \
        --os-type Linux \
        --output none
fi

echo "    - garantindo runtime Java 17 (o create às vezes aplica java|11, e classes"
echo "      compiladas para 17 travam o worker até o timeout, sem log de erro)"
az functionapp config set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$FUNCTION_APP" \
    --linux-fx-version "Java|17" \
    --output none

echo "    - habilitando credenciais básicas de publicação SCM (o quarkus:deploy usa; o Azure cria desabilitado)"
az resource update \
    --resource-group "$RESOURCE_GROUP" \
    --namespace Microsoft.Web \
    --resource-type basicPublishingCredentialsPolicies \
    --parent "sites/$FUNCTION_APP" \
    --name scm \
    --set properties.allow=true \
    --output none

echo "    - App Settings (variáveis de ambiente da função)"
DB_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${DB_NAME}?useSSL=true"
az functionapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$FUNCTION_APP" \
    --settings \
        "DB_URL=$DB_URL" \
        "DB_USERNAME=$SQL_ADMIN_USER" \
        "DB_PASSWORD=$SQL_ADMIN_PASSWORD" \
        "AZURE_STORAGE_CONNECTION_STRING=$STORAGE_CONNECTION_STRING" \
    --output none

echo
echo "============================================================"
echo "Provisionamento concluído."
echo
echo "  MySQL:    ${MYSQL_HOST} (db: $DB_NAME)"
echo "  Storage:  $STORAGE_ACCOUNT (container: $BLOB_CONTAINER)"
echo "  Function: $FUNCTION_APP"
echo
echo "Próximos passos:"
echo "  1. Deploy do código (na pasta fn-relatorio-semanal/):"
echo "       ./mvnw clean package quarkus:deploy"
echo "  2. Teste manual (sem esperar segunda 08:00) — dispara o timer via admin API:"
echo "       az rest --method post \\"
echo "         --url \"https://${FUNCTION_APP}.azurewebsites.net/admin/functions/relatorioSemanal\" \\"
echo "         --headers \"x-functions-key=\$(az functionapp keys list -g $RESOURCE_GROUP -n $FUNCTION_APP --query masterKey -o tsv)\" \\"
echo "         --body '{}'"
echo "  3. Conferir o blob gerado:"
echo "       az storage blob list --container-name $BLOB_CONTAINER \\"
echo "         --connection-string \"\$STORAGE_CONNECTION_STRING\" -o table"
echo "============================================================"