#!/usr/bin/env bash
#
# Run the payment-api application locally but pointed at the AAT or Demo
# environments.
#
# All environment variables and secrets are sourced from
# charts/payment-api/values.yaml:
#   - plain env vars (java.environment) are exported, with the Helm template
#     `{{ .Values.global.environment }}` substituted for the target environment
#   - secrets (java.keyVaults.ccpay.secrets) are fetched at runtime from the
#     ccpay-<environment> Key Vault using the Azure CLI, mirrored into a temp
#     Spring Cloud Config Tree (SPRING_CONFIG_IMPORT) exactly like the
#     /mnt/secrets/ccpay tree in the cluster, and never persisted or committed.
#
# DB connection comes from the POSTGRES_* secrets, so the app connects to the
# cloud database (e.g. <service>-postgres-db-v*-aat.postgres.database.azure.com).
#
# Usage:
#   ./scripts/run-locally.sh [-e <env>]
#
# -e <env>   target environment: aat (default), demo, perftest, ithc
#
# Prerequisites:
#   - Connected to the VPN (so the .internal service URLs and the Azure
#     Postgres host resolve)
#   - Logged in to the Azure CLI:  `az login`

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
VALUES_YAML="${ROOT_DIR}/charts/payment-api/values.yaml"

ENVIRONMENT="aat"

usage() {
    sed -n '2,26p' "$0" | sed 's/^# \{0,1\}//'
}

case "${1:-}" in
    --help|-h) usage; exit 0 ;;
esac

while getopts ":e:h" opt; do
    case "$opt" in
        e) ENVIRONMENT="$OPTARG" ;;
        h) usage; exit 0 ;;
        \?) echo "ERROR: unknown option -$OPTARG" >&2; usage >&2; exit 1 ;;
        :) echo "ERROR: option -$OPTARG requires an argument" >&2; usage >&2; exit 1 ;;
    esac
done
shift $((OPTIND - 1))

case "${ENVIRONMENT}" in
    aat|demo|perftest|ithc) ;;
    *)
        echo "ERROR: unsupported environment '${ENVIRONMENT}'. Use aat, demo, perftest or ithc." >&2
        usage
        exit 1
        ;;
esac

VAULT="ccpay-${ENVIRONMENT}"

if [[ ! -f "${VALUES_YAML}" ]]; then
    echo "ERROR: values file not found at ${VALUES_YAML}" >&2
    exit 1
fi

if ! command -v az >/dev/null 2>&1; then
    echo "ERROR: Azure CLI not found. Install it and run 'az login'." >&2
    exit 1
fi

az account show >/dev/null 2>&1 || {
    echo "ERROR: Not logged into Azure. Run 'az login' (and connect to the VPN) first." >&2
    exit 1
}

trim() {
    local s="${1#"${1%%[![:space:]]*}"}"
    s="${s%"${s##*[![:space:]]}"}"
    printf '%s' "$s"
}

vault_secret() {
    az keyvault secret show \
        --vault-name "$1" \
        --name "$2" \
        --query value \
        --output tsv 2>/dev/null
}

# ---------------------------------------------------------------------------
# Plain env vars from the java.environment block (with Helm template replaced)
# ---------------------------------------------------------------------------
env_block() {
    awk '
        /^  environment:/ { in_env=1; next }
        in_env && /^  [a-zA-Z0-9_]/ { in_env=0 }
        in_env { print }
    ' "${VALUES_YAML}"
}

env_var_count=0
while IFS= read -r line; do
    [[ -z "${line}" || "${line}" == \#* ]] && continue
    key="$(trim "${line%%:*}")"
    value="${line#*: }"
    value="$(trim "${value}")"
    value="${value#\"}"; value="${value%\"}"
    value="${value#\'}"; value="${value%\'}"
    export "${key}=${value}"
    env_var_count=$((env_var_count + 1))
done < <(env_block | sed "s|{{ .Values.global.environment }}|${ENVIRONMENT}|g")

# ---------------------------------------------------------------------------
# Secrets from the java.keyVaults block -> Spring Cloud Config Tree
# ---------------------------------------------------------------------------
SECRETS_DIR="$(mktemp -d "${TMPDIR:-/tmp}/payment-api-secrets.XXXXXX")"
trap 'rm -rf "${SECRETS_DIR}"' EXIT

secret_count=0
while IFS= read -r name_line && IFS= read -r alias_line; do
    name="$(trim "${name_line#*name: }")"
    alias="$(trim "${alias_line#*alias: }")"
    value="$(vault_secret "${VAULT}" "${name}")"
    if [[ -z "${value}" ]]; then
        echo "WARNING: could not fetch '${name}' from vault '${VAULT}' (env '${alias}' left unset)" >&2
        continue
    fi
    printf '%s' "${value}" > "${SECRETS_DIR}/${alias}"
    secret_count=$((secret_count + 1))
done < <(grep -E '^[[:space:]]*- name:|^[[:space:]]*alias:' "${VALUES_YAML}")

if [[ "${secret_count}" -eq 0 ]]; then
    echo "ERROR: no secrets were fetched from vault '${VAULT}'. Check permissions and VPN." >&2
    exit 1
fi

export SPRING_CONFIG_IMPORT="configtree:${SECRETS_DIR}/"
unset SPRING_PROFILES_ACTIVE 2>/dev/null || true

# Jenkins agents run in UTC; enforce the same locally for consistent timestamps.
export TZ=UTC
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Duser.timezone=UTC"
export JAVA_TOOL_OPTIONS

# ---------------------------------------------------------------------------
# Launch
# ---------------------------------------------------------------------------
cat <<EOF
Environment : ${ENVIRONMENT}
Vault      : ${VAULT}
Env vars   : ${env_var_count} exported from charts/payment-api/values.yaml
Secrets    : ${secret_count} materialised from ${VAULT} into Spring config tree
DB target  : \${POSTGRES_HOST}:\${POSTGRES_PORT} (liquibase disabled, from values.yaml)
Starting payment-api...
EOF

cd "${ROOT_DIR}"
./gradlew bootRun "$@"