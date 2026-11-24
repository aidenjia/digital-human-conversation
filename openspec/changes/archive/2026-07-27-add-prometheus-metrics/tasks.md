## 1. Dependencies and Actuator config

- [x] 1.1 Add `micrometer-registry-prometheus` to `pom.xml`
- [x] 1.2 Update `application.yml` management: expose `health,info,prometheus`; set `management.metrics.tags.application`; enable `http.server.requests` percentile histogram
- [x] 1.3 Confirm no separate `management.server.port` is introduced; keep shared `server.port`

## 2. Tests

- [x] 2.1 Add/extend IT for `GET /actuator/prometheus` → 200 and body contains JVM and/or HTTP metric series plus `application` label
- [x] 2.2 Keep existing `/actuator/health` IT green; run `mvn spotless:apply` then `mvn verify`

## 3. Deploy example and docs

- [x] 3.1 Add `deploy/k8s/servicemonitor.example.yaml` scraping `/actuator/prometheus` on the app HTTP port (comment that labels/namespace must be aligned)
- [x] 3.2 Update `README.md` with Prometheus path, same-port convention, and Ingress must not expose `/actuator` for public metrics
- [x] 3.3 Do not change `docs/convoai-client-integration.md` (no client API impact)
