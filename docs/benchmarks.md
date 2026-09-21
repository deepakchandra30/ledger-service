# Measurements

Fill these in from your own runs. They are the numbers that belong on a CV, so
record the machine and the command alongside each one.

| Measurement | How to get it | Result |
|---|---|---|
| Transfers per second | `k6 run load/post-transfers.js` | |
| p99 post latency | Grafana panel "Post latency p99" | |
| Concurrent transfers, conflict rate | `./gradlew :api:test --tests '*TransferConcurrencyIT'` | |
| Test count | `./gradlew test` summary | |
| Line coverage | `api/build/reports/jacoco/test/html/index.html` | |
| Outbox drain latency | time between `created_at` and `published_at` | |

Machine: (e.g. M2 Pro, 16GB, Docker Desktop 4.x)
