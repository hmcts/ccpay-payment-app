#!/usr/bin/env bash
#
# Run the payment functional tests (Serenity) against a deployed AAT environment.
#
# Environment variables and secrets are sourced from the Jenkinsfile_CNP pipeline.
# Secret VALUES are fetched from Azure Key Vault at runtime and never persisted or
# committed - only the vault/secret names and target env var names live in this file.
#
# Usage:
#   ./scripts/run-functional-tests.sh [-e <env>]                  # run all functional tests
#   ./scripts/run-functional-tests.sh [-e <env>] FooTest          # run a single test class
#   ./scripts/run-functional-tests.sh [-e <env>] FooTest.method   # run a single test method
#
# -e <env>   target environment: aat (default), demo, perftest, ithc
#
# Any of the TEST_URL, REFUND_API_URL, CCPAY_BULK_SCANNING_API_URL, S2S_URL,
# IDAM_API_URL, AUTH_IDAM_CLIENT_BASEURL, REF_DATA_API_URL or OAUTH2_REDIRECT_URI
# variables can be overridden in the environment; otherwise they are derived from -e.
#
# Prerequisites:
#   - Connected to the HMRC VPN (so the internal .internal URLs resolve)
#   - Logged in to the Azure CLI:  `az login`

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration (mirrors Jenkinsfile_CNP + charts/payment-api/values.yaml)
# ---------------------------------------------------------------------------
ENVIRONMENT="aat"

usage() {
    sed -n '2,22p' "$0" | sed 's/^# \{0,1\}//'
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

VAULT="ccpay-${ENVIRONMENT}"                       # = ccpay-aat

# All URLs default to the target environment but can be overridden in the
# environment (e.g. for PR/preview endpoints).
# test.url is derived from the ingressHost in charts/payment-api/values.yaml
#   java.ingressHost: payment-api-{{ .Values.global.environment }}.service.core-compute-{{ .Values.global.environment }}.internal
TEST_URL="${TEST_URL:-http://payment-api-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal}"

# From Jenkinsfile_CNP `before('functionalTest:aat')`
REFUND_API_URL="${REFUND_API_URL:-http://ccpay-refunds-api-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal}"
CCPAY_BULK_SCANNING_API_URL="${CCPAY_BULK_SCANNING_API_URL:-http://ccpay-bulkscanning-api-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal}"

# Services the functional tests call directly (application-functional-tests.properties).
S2S_URL="${S2S_URL:-http://rpe-service-auth-provider-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal}"
IDAM_API_URL="${IDAM_API_URL:-https://idam-api.${ENVIRONMENT}.platform.hmcts.net}"
AUTH_IDAM_CLIENT_BASEURL="${AUTH_IDAM_CLIENT_BASEURL:-https://idam-api.${ENVIRONMENT}.platform.hmcts.net}"
REF_DATA_API_URL="${REF_DATA_API_URL:-http://rd-professional-api-${ENVIRONMENT}.service.core-compute-${ENVIRONMENT}.internal}"
OAUTH2_REDIRECT_URI="${OAUTH2_REDIRECT_URI:-https://moneyclaims.${ENVIRONMENT}.platform.hmcts.net}"

export TEST_URL
export REFUND_API_URL
export CCPAY_BULK_SCANNING_API_URL
export S2S_URL
export IDAM_API_URL
export AUTH_IDAM_CLIENT_BASEURL
export REF_DATA_API_URL
export OAUTH2_REDIRECT_URI

echo "Target environment: ${ENVIRONMENT}"
echo "  TEST_URL:                 ${TEST_URL}"
echo "  S2S_URL:                  ${S2S_URL}"
echo "  IDAM_API_URL:             ${IDAM_API_URL}"
echo "  REF_DATA_API_URL:         ${REF_DATA_API_URL}"

# Jenkins agents run in UTC; enforce the same on the local JVM (and its forked
# test JVMs). Otherwise date-based assertions (e.g. verifyDate in
# ServiceRequestFunctionalTests) drift by the local daylight-saving offset, e.g.
# BST (+1). Inherited by the Gradle daemon and the forked test JVM.
export TZ=UTC
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Duser.timezone=UTC"
export JAVA_TOOL_OPTIONS

SINGLE_TEST="${1:-}"

# ---------------------------------------------------------------------------
# Azure check
# ---------------------------------------------------------------------------
az account show >/dev/null 2>&1 || {
    echo "ERROR: Not logged into Azure. Run 'az login' (and connect to the VPN) first." >&2
    exit 1
}

# ---------------------------------------------------------------------------
# Secret helpers - pull a secret from the vault into stdout, never to a file
# ---------------------------------------------------------------------------
vault_secret() {
    az keyvault secret show \
        --vault-name "$1" \
        --name "$2" \
        --query value \
        --output tsv 2>/dev/null
}

require() {
    # require <envVarName> <vaultName> <secretName>
    local env_name="$1" value
    value="$(vault_secret "$2" "$3")"
    if [ -z "$value" ]; then
        echo "WARNING: could not fetch '$3' from vault '$2' (env $1 left unset)" >&2
        return 0
    fi
    export "$env_name=$value"
}

# ---------------------------------------------------------------------------
# Load secrets from Key Vault (from the `secrets` block in Jenkinsfile_CNP)
# ---------------------------------------------------------------------------
require S2S_SERVICE_SECRET                 "${VAULT}" cmc-service-secret
require PAYBUBBLE_S2S_SERVICE_SECRET        "${VAULT}" paybubble-s2s-secret
require OAUTH2_CLIENT_SECRET                "${VAULT}" citizen-oauth-client-secret
require GENERATED_USER_EMAIL_PATTERN        "${VAULT}" freg-idam-generated-user-email-pattern
require TEST_USER_PASSWORD                  "${VAULT}" freg-idam-test-user-password
require GOV_PAY_AUTH_KEY_CMC                "${VAULT}" gov-pay-keys-cmc
require PCI_PAL_ANTENNA_CLIENT_SECRET       "${VAULT}" pci-pal-antenna-client-secret
require PCI_PAL_KERV_CLIENT_SECRET          "${VAULT}" pci-pal-kerv-client-secret
require IDAM_PAYBUBBLE_CLIENT_SECRET        "${VAULT}" paybubble-idam-client-secret
require S2S_PAYMENT_APP_SERVICE_SECRET      "${VAULT}" payment-s2s-secret
require IDAM_RD_PROFESSIONAL_CLIENT_SECRET "${VAULT}" ref-data-professional-client-secret
require PROBATE_SOLICITOR_USER              "${VAULT}" probate-solicitor-username
require PROBATE_SOLICITOR_PASSWORD          "${VAULT}" probate-solicitor-password
require LIBERATA_API_REALTIME_ACCOUNT_USERNAME "${VAULT}" liberata-api-realtime-user
require LIBERATA_API_REALTIME_ACCOUNT_PASSWORD "${VAULT}" liberata-api-realtime-password

# Sanity check the secrets the tests need most
for env_var in OAUTH2_CLIENT_SECRET TEST_USER_PASSWORD S2S_SERVICE_SECRET; do
    if [ -z "${!env_var:-}" ]; then
        echo "ERROR: required secret '$env_var' is empty - cannot run functional tests." >&2
        exit 1
    fi
done

# ---------------------------------------------------------------------------
# Run the tests
# ---------------------------------------------------------------------------
if [ -n "$SINGLE_TEST" ]; then
    echo "Running single functional test: $SINGLE_TEST"
    ./gradlew :payment-api:functionalTest \
        --tests "uk.gov.hmcts.payment.functional.${SINGLE_TEST}" \
        --rerun-tasks
else
    echo "Running all functional tests"
    ./gradlew --console plain functional
fi
