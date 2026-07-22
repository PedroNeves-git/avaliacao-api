#!/usr/bin/env bash
# ============================================================================
# Provisionamento Azure — fn-relatorio-semanal (Tech Challenge FIAP, Fase 4)
#
# Cria, na ordem:
#   1. Resource Group
#   2. Azure SQL Database (server lógico + database + firewall + schema)
#   3. Storage Account (AzureWebJobsStorage + container "relatorios")
#   4. Function App (Java 17, plano Consumption) + App Settings
#
# Por que Azure SQL (e não MySQL): o schema canônico do repositório
# (db/schema.sql) é T-SQL, e o provider Microsoft.DBforMySQL retorna
# InternalServerError em todas as regiões que a policy da assinatura
# Azure for Students permite — MySQL é improvisionável nessa assinatura.
#
# Pré-requisitos:
#   - Azure CLI instalado e logado (az login)
#   - Senha do banco na variável de ambiente SQL_ADMIN_PASSWORD
#     (se ausente, é pedida interativamente sem eco no terminal)
#   - sqlcmd (opcional, para aplicar o schema; presente no Cloud Shell)
#
# Uso:
#   export SQL_ADMIN_PASSWORD='<senha forte>'
#   ./provision.sh
#
# O script é seguro de re-executar: recursos já existentes são reaproveitados.
# Depois dele, o deploy do código é: az login && ./mvnw clean package quarkus:deploy
# ============================================================================
set -euo pipefail

# ── Configuração (mesmos valores do application.properties) ─────────────────
RESOURCE_GROUP="rg-tech-challenge"
LOCATION="southcentralus"
FUNCTION_APP="fn-relatorio-semanal"

# Azure SQL não aceita criação em todas as regiões para assinaturas de
# estudante ('RegionDoesNotAllowProvisioning'); centralus foi validada.
# Pode ser sobrescrita: export SQL_LOCATION=eastus2
SQL_LOCATION="${SQL_LOCATION:-centralus}"

# Nomes globais precisam ser únicos no Azure inteiro; o sufixo torna isso
# reprodutível por assinatura sem colidir com nomes de outros alunos. O nome
# do SQL Server inclui a região porque um create que falha "prende" o nome à
# região tentada (registro fantasma no ARM).
SUFFIX=$(az account show --query id -o tsv | cut -c1-8)
SQL_SERVER="sql-tech-challenge-${SUFFIX}-${SQL_LOCATION:0:4}"
STORAGE_ACCOUNT="sttechchallenge${SUFFIX}"   # só minúsculas/números, máx. 24 chars
DB_NAME="feedback"
SQL_ADMIN_USER="fiapadmin"
BLOB_CONTAINER="relatorios"

# Senha via variável de ambiente ou, na falta dela, pedida interativamente
# (sem eco no terminal e sem passar por argumento, para não vazar no histórico).
# Aceita MYSQL_ADMIN_PASSWORD como alias legado.
SQL_ADMIN_PASSWORD="${SQL_ADMIN_PASSWORD:-${MYSQL_ADMIN_PASSWORD:-}}"
if [[ -z "$SQL_ADMIN_PASSWORD" ]]; then
    read -r -s -p "Senha do admin do SQL (SQL_ADMIN_PASSWORD): " SQL_ADMIN_PASSWORD
    echo
fi
: "${SQL_ADMIN_PASSWORD:?Senha vazia — defina SQL_ADMIN_PASSWORD ou informe quando solicitado}"

echo "==> Assinatura: $(az account show --query name -o tsv)"
echo "==> Recursos: RG=$RESOURCE_GROUP | SQL=$SQL_SERVER | Storage=$STORAGE_ACCOUNT | App=$FUNCTION_APP"

# ── 1. Resource Group ────────────────────────────────────────────────────────
echo "==> [1/4] Resource Group"
# Se o RG já existe, reaproveita a região dele (um RG não pode ser recriado em
# outra região, e os demais recursos devem ficar na mesma região do grupo).
if az group show --name "$RESOURCE_GROUP" --output none 2>/dev/null; then
    LOCATION=$(az group show --name "$RESOURCE_GROUP" --query location -o tsv)
    echo "    - já existe em '$LOCATION' — reaproveitando região"
else
    az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
fi

# ── 2. Azure SQL Database ────────────────────────────────────────────────────
echo "==> [2/4] Azure SQL (server lógico + database Basic — menor custo)"
if az sql server show --resource-group "$RESOURCE_GROUP" --name "$SQL_SERVER" --output none 2>/dev/null; then
    echo "    - server já existe — reaproveitando"
else
    echo "    - criando server em '$SQL_LOCATION'"
    az sql server create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$SQL_SERVER" \
        --location "$SQL_LOCATION" \
        --admin-user "$SQL_ADMIN_USER" \
        --admin-password "$SQL_ADMIN_PASSWORD" \
        --output none
fi

echo "    - database '$DB_NAME' (tier Basic, 2 GB)"
az sql db create \
    --resource-group "$RESOURCE_GROUP" \
    --server "$SQL_SERVER" \
    --name "$DB_NAME" \
    --edition Basic \
    --capacity 5 \
    --max-size 2GB \
    --output none

echo "    - firewall: liberando serviços do Azure (Function App -> banco)"
az sql server firewall-rule create \
    --resource-group "$RESOURCE_GROUP" \
    --server "$SQL_SERVER" \
    --name "AllowAzureServices" \
    --start-ip-address 0.0.0.0 \
    --end-ip-address 0.0.0.0 \
    --output none

echo "    - firewall: liberando IP desta máquina (para aplicar o schema)"
# -4 força IPv4: o firewall do Azure SQL não aceita endereços IPv6
MY_IP=$(curl -4 -s https://ifconfig.me)
az sql server firewall-rule create \
    --resource-group "$RESOURCE_GROUP" \
    --server "$SQL_SERVER" \
    --name "dev-$(whoami)" \
    --start-ip-address "$MY_IP" \
    --end-ip-address "$MY_IP" \
    --output none

SQL_FQDN="${SQL_SERVER}.database.windows.net"
SCHEMA_FILE="$(cd "$(dirname "$0")/../.." && pwd)/db/schema.sql"
echo "    - aplicando schema (db/schema.sql — idempotente, usa IF NOT EXISTS)"
if command -v sqlcmd >/dev/null 2>&1; then
    sqlcmd -S "$SQL_FQDN" -d "$DB_NAME" -U "$SQL_ADMIN_USER" -P "$SQL_ADMIN_PASSWORD" \
        -i "$SCHEMA_FILE" -b
else
    echo "      AVISO: 'sqlcmd' não encontrado — aplique o schema manualmente:"
    echo "        sqlcmd -S $SQL_FQDN -d $DB_NAME -U $SQL_ADMIN_USER -P '<senha>' -i db/schema.sql"
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
DB_URL="jdbc:sqlserver://${SQL_FQDN}:1433;databaseName=${DB_NAME};encrypt=true;trustServerCertificate=false;loginTimeout=30"
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
echo "  SQL:      ${SQL_FQDN} (db: $DB_NAME)"
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
