# scripts/

Helper scripts for local development and testing of the payment-api application.

## Prerequisites (common)

- Bash
- Azure CLI (`brew install azure-cli`) and `az login`
- Connected to the HMCTS VPN so the `.internal` service URLs and the Azure
  Postgres host resolve

`tokens.sh`, `paybubble-probate.sh` and `paybubble-divorce.sh` also need curl.

All scripts accept the common option:

```
-e <env>   target environment: aat (default), demo, perftest, ithc
```

Secrets are fetched from the `ccpay-<env>` Azure Key Vault at runtime using
`az keyvault secret show` and are never persisted or committed.

---

## run-locally.sh

Runs the payment-api Spring Boot application **locally** (via `./gradlew bootRun`)
but pointed at the target environment. The local app uses the cloud services
(IdAM, S2S, fees, PCI-PAL, GovPay, mail, etc.) and the **cloud database** of the
target environment, so you can debug against realistic data, e.g. interactively
replay a service request.

```
./scripts/run-locally.sh [-e <env>]          # default: aat
./scripts/run-locally.sh --help
```

What it does:

1. Reads `charts/payment-api/values.yaml`:
   - `java.environment` block - every `KEY: value` is exported as an
     environment variable, with the Helm template `{{ .Values.global.environment }}`
     substituted for the target environment (`aat`, `demo`, `perftest`, `ithc`).
   - `java.keyVaults.ccpay.secrets` block - each `name`/`alias` pair is fetched
     from the `ccpay-<environment>` Key Vault using `az keyvault secret show`.
2. Replicates the cluster's secret storage: the vault secret values are written
   into a temporary **Spring Cloud Config Tree** (a `mktemp -d` directory, one
   file per `alias`) and exposed via `SPRING_CONFIG_IMPORT=configtree:<dir>/`.
   This is the same mechanism as
   `spring.config.import=optional:configtree:/mnt/secrets/ccpay/` in
   `application.properties`, so both uppercase placeholders (`${POSTGRES_*}`)
   and dotted `@Value` properties (e.g. `pci-pal.api.url`) resolve correctly.
   The temp directory is removed when the script exits.
3. Launches the app: `./gradlew bootRun` (root project).

Notes:

- DB connection comes from the `POSTGRES_*` secrets, so the app connects to the
  cloud database, e.g. `<service>-postgres-db-v15-aat.postgres.database.azure.com:5432`.
- Liquibase is **disabled** (`SPRING_LIQUIBASE_ENABLED=false` and
  `RUN_DB_MIGRATION_ON_STARTUP=false` from values.yaml), so no migrations are
  run against the shared environment database.

---

## run-functional-tests.sh

Runs the payment-api **Serenity functional tests** against a **deployed**
environment (default AAT).

```
./scripts/run-functional-tests.sh [-e <env>]              # run all functional tests
./scripts/run-functional-tests.sh [-e <env>] FooTest      # run a single test class
./scripts/run-functional-tests.sh --help
```

What it does:

1. Mirrors the Jenkins (`Jenkinsfile_CNP`) configuration for the environment:
   - `VAULT=ccpay-<env>`.
   - `TEST_URL` derived from the ingressHost in `charts/payment-api/values.yaml`
     (`http://payment-api-<env>.service.core-compute-<env>.internal`).
   - `REFUND_API_URL` and `CCPAY_BULK_SCANNING_API_URL` (the
     `before('functionalTest:aat')` step, generalised per environment).
2. Streams the secrets from the `ccpay-<env>` vault (the `secrets` block in
   `Jenkinsfile_CNP`) into environment variables, e.g. `OAUTH2_CLIENT_SECRET`,
   `TEST_USER_PASSWORD`, `S2S_SERVICE_SECRET`.
3. Forces `TZ=UTC` (and `-Duser.timezone=UTC`) so date-based assertions don't
   drift by the local daylight-saving offset.
4. Runs the Serenity tests:
   - single class: `./gradlew :payment-api:functionalTest --tests "uk.gov.hmcts.payment.functional.<Class>" --rerun-tasks`
   - all: `./gradlew --console plain functional`

Notes:

- Requires the VPN (talks to `.internal` hosts) and `az login`.
- Reports land under `api/target/site/serenity`.
- The invocation order is `-e <env>` first, then the optional test class name,
  e.g. `./scripts/run-functional-tests.sh -e demo ServiceRequestFunctionalTests`.

---

## tokens.sh

Obtains a **service-to-service (S2S) token** and an **IdAM OAuth2 access token**
for a target environment, for manual API testing with curl.

```
./scripts/tokens.sh [-e <env>] [<S2S_MICROSERVICE>] [<USER_EMAIL>] [<USER_PASSWORD>]
./scripts/tokens.sh --help
```

Defaults, resolved from the `ccpay-<env>` Key Vault at runtime:

- `<S2S_MICROSERVICE>` - the S2S microservice name, default `ccpay_bubble`.
- `<USER_EMAIL>` - defaults to the `probate-caseworker-username` secret unless
  passed explicitly.
- `<USER_PASSWORD>` - defaults to the `probate-caseworker-password` secret
  unless passed explicitly.
- `CLIENT_SECRET` - always read from the `paybubble-idam-client-secret` secret.

What it does:

1. Fetches the S2S token from the `testing-support` lease endpoint:
   `POST http://rpe-service-auth-provider-<env>.service.core-compute-<env>.internal/testing-support/lease`
   with body `{"microservice":"<S2S_MICROSERVICE>"}`.
2. Fetches the IdAM access token (password grant):
   `POST https://idam-api.<env>.platform.hmcts.net:443/o/token` with
   `client_id=paybubble`, `redirect_uri=https://paybubble.<env>.platform.hmcts.net/oauth2/callback`,
   `grant_type=password`, the client secret, the user credentials and
   `scope=openid profile roles`.
3. Prints both tokens so you can pass them into curl
   (`ServiceAuthorization: <s2s>`, `Authorization: Bearer <idam>`):

```
S2S MICROSERVICE: ccpay_bubble

S2S TOKEN:
<token>

IDAM USER: probatedraftcasecreator@mailinator.com

IDAM TOKEN:
<token>
```

---

## paybubble-probate.sh

Wrapper around `tokens.sh` that mints PayBubble S2S + IdAM tokens using the
**probate** caseworker credentials, resolving them from the vault itself.

```
./scripts/paybubble-probate.sh [-e <env>]
./scripts/paybubble-probate.sh --help
```

Reads `probate-caseworker-username` and `probate-caseworker-password` from the
`ccpay-<env>` Key Vault and delegates to
`tokens.sh -e <env> ccpay_bubble <user> <password>`.

---

## paybubble-divorce.sh

Wrapper around `tokens.sh` that mints PayBubble S2S + IdAM tokens using the
**divorce** caseworker credentials.

```
./scripts/paybubble-divorce.sh [-e <env>]
./scripts/paybubble-divorce.sh --help
```

Reads `divorce-caseworker-username` and `divorce-caseworker-password` from the
`ccpay-<env>` Key Vault and delegates to
`tokens.sh -e <env> ccpay_bubble <user> <password>`.