#!/usr/bin/env bash
#
# Obtain an S2S token and an IdAM OAuth2 access token for a target environment.
#
# Secrets are fetched from the ccpay-<env> Key Vault at runtime using the Azure
# CLI and are never persisted or committed:
#   - CLIENT_SECRET is always read from the `paybubble-idam-client-secret` secret
#   - USER_EMAIL / USER_PASSWORD default to the `probate-caseworker-username` /
#     `probate-caseworker-password` secrets unless passed explicitly
#
# Usage:
#   ./scripts/tokens.sh [-e <env>] [<S2S_MICROSERVICE>] [<USER_EMAIL>] [<USER_PASSWORD>]
#
# -e <env>            target environment: aat (default), demo, perftest, ithc
# <S2S_MICROSERVICE>  S2S microservice name, default: ccpay_bubble
# <USER_EMAIL>        IdAM username (optional; falls back to probate-caseworker-username)
# <USER_PASSWORD>     IdAM password (optional; falls back to probate-caseworker-password)
#
# Prerequisites:
#   - Connected to the VPN (so the .internal S2S host resolves)
#   - Logged in to the Azure CLI:  `az login`

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ENVIRONMENT="aat"

usage() {
    sed -n '2,21p' "$0" | sed 's/^# \{0,1\}//'
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

S2S_MICROSERVICE="${1:-ccpay_bubble}"
USER_EMAIL="${2:-}"
USER_PASSWORD="${3:-}"

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

CLIENT_SECRET="$(vault_secret "${VAULT}" paybubble-idam-client-secret)"
if [[ -z "${CLIENT_SECRET}" ]]; then
    echo "ERROR: could not fetch 'paybubble-idam-client-secret' from vault '${VAULT}'." >&2
    exit 1
fi

if [[ -z "${USER_EMAIL}" ]]; then
    USER_EMAIL="$(vault_secret "${VAULT}" probate-caseworker-username)"
    if [[ -z "${USER_EMAIL}" ]]; then
        echo "ERROR: could not fetch 'probate-caseworker-username' from vault '${VAULT}'." >&2
        exit 1
    fi
fi

if [[ -z "${USER_PASSWORD}" ]]; then
    USER_PASSWORD="$(vault_secret "${VAULT}" probate-caseworker-password)"
    if [[ -z "${USER_PASSWORD}" ]]; then
        echo "ERROR: could not fetch 'probate-caseworker-password' from vault '${VAULT}'." >&2
        exit 1
    fi
fi

echo "Fetching tokens for ${ENVIRONMENT}..."

S2S_URL="http://rpe-service-auth-provider-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal/testing-support/lease"
s2s_token="$(curl -sS -X POST "${S2S_URL}" \
    -H "Content-Type: application/json" \
    -d "{\"microservice\":\"${S2S_MICROSERVICE}\"}")"
if [[ -z "${s2s_token}" ]]; then
    echo "ERROR: S2S token request to '${S2S_URL}' returned nothing." >&2
    exit 1
fi

IDAM_TOKEN_URL="https://idam-api.${ENVIRONMENT}.platform.hmcts.net:443/o/token"
REDIRECT_URI="https://paybubble.${ENVIRONMENT}.platform.hmcts.net/oauth2/callback"
idam_response="$(curl -sS -X POST "${IDAM_TOKEN_URL}" \
    -H "Accept: *" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "client_id=paybubble" \
    --data-urlencode "redirect_uri=${REDIRECT_URI}" \
    --data-urlencode "grant_type=password" \
    --data-urlencode "client_secret=${CLIENT_SECRET}" \
    --data-urlencode "password=${USER_PASSWORD}" \
    --data-urlencode "username=${USER_EMAIL}" \
    --data-urlencode "scope=openid profile roles")"

idam_token="$(printf '%s' "${idam_response}" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
if [[ -z "${idam_token}" ]]; then
    echo "ERROR: could not parse an access_token from the IdAM response." >&2
    echo "${idam_response}" >&2
    exit 1
fi

echo ""
echo "S2S MICROSERVICE: ${S2S_MICROSERVICE}"
echo ""
echo "S2S TOKEN:"
echo "${s2s_token}"
echo ""
echo "IDAM USER: ${USER_EMAIL}"
echo ""
echo "IDAM TOKEN:"
echo "${idam_token}"
echo ""