#!/usr/bin/env bash
#
# Mint PayBubble S2S + IdAM tokens using the divorce caseworker credentials.
#
# Usage:
#   ./scripts/paybubble-divorce.sh [-e <env>]
#
# -e <env>   target environment: aat (default), demo, perftest, ithc
#
# Resolves the divorce caseworker username/password from the ccpay-<env> Key
# Vault and delegates to tokens.sh.
#
# Prerequisites:
#   - Connected to the VPN (so the .internal S2S host resolves)
#   - Logged in to the Azure CLI:  `az login`

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ENVIRONMENT="aat"

usage() {
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
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
        exit 1
        ;;
esac

VAULT="ccpay-${ENVIRONMENT}"

if ! command -v az >/dev/null 2>&1; then
    echo "ERROR: Azure CLI not found. Install it and run 'az login'." >&2
    exit 1
fi

az account show >/dev/null 2>&1 || {
    echo "ERROR: Not logged into Azure. Run 'az login' (and connect to the VPN) first." >&2
    exit 1
}

vault_secret() {
    az keyvault secret show \
        --vault-name "$1" \
        --name "$2" \
        --query value \
        --output tsv 2>/dev/null
}

USER_EMAIL="$(vault_secret "${VAULT}" divorce-caseworker-username)"
USER_PASSWORD="$(vault_secret "${VAULT}" divorce-caseworker-password)"
if [[ -z "${USER_EMAIL}" || -z "${USER_PASSWORD}" ]]; then
    echo "ERROR: could not fetch divorce caseworker credentials from vault '${VAULT}'." >&2
    exit 1
fi

exec "${SCRIPT_DIR}/tokens.sh" -e "${ENVIRONMENT}" ccpay_bubble "${USER_EMAIL}" "${USER_PASSWORD}"