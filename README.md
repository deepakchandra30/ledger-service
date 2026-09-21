# Ledger

A double-entry transaction ledger with asynchronous settlement. Money moves
through a REST API, every posting writes balanced debit and credit entries in
one database transaction, and settlement happens off a Kafka event published
through a transactional outbox.

Built to be correct under concurrency and observable in production, not to be a
CRUD demo.

## Stack

Java 21, Spring Boot 3, Postgres with Flyway, Kafka, Resilience4j, Micrometer
with Prometheus and Grafana, Spring Security with OAuth2 and JWT, Testcontainers,
React with TypeScript, Terraform, GitHub Actions.

## What it demonstrates

| Concern | Where |
|---|---|
| Domain modelling | `Account`, `LedgerEntry`, `Transfer` with an enforced invariant |
| Concurrency | Optimistic locking, lost-update test with 200 concurrent transfers |
| Transactional SQL | Explicit isolation level, indexes justified by query plans |
| Testing | Unit, integration against real Postgres and Kafka, property-style invariant test |
| Messaging | Transactional outbox, consumer groups, idempotent consumer |
| Resilience | Retries, circuit breaker, idempotency keys |
| Observability | Metrics, traces, a provisioned Grafana dashboard |
| Security | JWT bearer auth, scope-separated endpoints, no secrets in the repo |
| Infrastructure | Terraform for RDS, MSK, ECR and Secrets Manager |
| CI/CD | Build, test, coverage, image push, Terraform validate |

## First time setup

The Gradle wrapper is not committed. Generate it once:

```bash
gradle wrapper --gradle-version 8.10
git add gradlew gradlew.bat gradle/wrapper && git commit -m "Add Gradle wrapper"
```

Get a development token from the local Keycloak realm:

```bash
export TOKEN=$(curl -s -X POST \
  http://localhost:8081/realms/ledger/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=ledger-cli \
  -d client_secret=local-dev-secret | jq -r .access_token)
```

## Run it locally

```bash
docker compose up -d postgres kafka prometheus grafana
./gradlew :api:bootRun          # http://localhost:8080
./gradlew :settlement:bootRun
cd web && npm install && npm run dev   # http://localhost:5173
```

Grafana is on http://localhost:3000 with the Ledger dashboard provisioned.

## Test it

```bash
./gradlew test                                        # unit
./gradlew :api:test --tests '*IT'                     # integration, needs Docker
./gradlew build jacocoTestReport                      # everything plus coverage
```

The integration tests start real Postgres and Kafka containers. Nothing is
mocked at the boundary.

## API

```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"from":"<uuid>","to":"<uuid>","amount":"25.0000"}'
```

| Status | Meaning |
|---|---|
| 201 | Posted |
| 409 | Account was contended, retry |
| 422 | Insufficient funds |
| 404 | No such account |

## Documentation

- `docs/architecture.md` — diagram, why double entry, failure modes
- `docs/adr/` — the three decisions worth arguing about

## Status

Core posting path, outbox, settlement consumer, security, observability and CI
are in place.
