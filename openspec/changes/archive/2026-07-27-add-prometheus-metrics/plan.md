# Add Prometheus Metrics Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development
> or openspec-apply-change to implement this plan task-by-task (TDD where noted).

**Goal:** Expose `/actuator/prometheus` on port 8080 with Micrometer, document scrape + Ingress boundary, and ship a ServiceMonitor example.

**Architecture:** Spring Boot Actuator + `micrometer-registry-prometheus` auto-config; same `server.port` as business/health; no new Java packages; platform scrapes via ClusterIP using the example ServiceMonitor.

**Tech Stack:** Java 21, Spring Boot 3.5, Micrometer Prometheus registry, MockMvc IT, K8s ServiceMonitor CRD example.

**Specs:** `openspec/changes/add-prometheus-metrics/specs/prometheus-metrics/spec.md`, `specs/docker-container-runtime/spec.md`

---

## Task 1: Dependencies and Actuator config

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1:** In `pom.xml` `<dependencies>`, after `spring-boot-starter-actuator`, add:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

- [ ] **Step 2:** Replace the management block in `application.yml` with:

```yaml
# Actuator：健康/信息/Prometheus（与业务共用 server.port；勿公网暴露 /actuator/prometheus）
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

- [ ] **Step 3:** Confirm no `management.server.port` key exists in any `application*.yml`.

- [ ] **Step 4:** Commit point (optional): `chore: add micrometer prometheus registry and expose endpoint`

---

## Task 2: Tests (TDD)

**Files:**
- Modify: `src/test/java/aidenjia/digitalhuman/conversation/WebApiFoundationIT.java`

- [ ] **Step 1:** Add failing test (before config merge if doing strict TDD; otherwise after Step 1–2 of Task 1):

```java
@Test
@DisplayName("GET /actuator/prometheus 在站点根路径可访问（无前缀）")
void actuatorPrometheusExposedAtRoot() throws Exception {
    mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")))
            .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("application=\"digital-human-conversation\"")));
}
```

(Adjust assertion if Boot naming uses `application="..."` on a different series; prefer matching `application=` tag presence.)

- [ ] **Step 2:** Optionally trigger one request first if histogram series are required in the same test; or assert `http_server_requests` after a prior `get("/")` / health call in the same method.

- [ ] **Step 3:** Run `mvn spotless:apply` then `mvn -Dtest=WebApiFoundationIT test` (or full `mvn verify`).

- [ ] **Step 4:** Commit point: `test: cover actuator prometheus endpoint`

---

## Task 3: Deploy example and docs

**Files:**
- Create: `deploy/k8s/servicemonitor.example.yaml`
- Modify: `README.md`

- [ ] **Step 1:** Create `deploy/k8s/servicemonitor.example.yaml`:

```yaml
# EXAMPLE ONLY — align metadata.namespace, metadata.labels, and spec.selector
# with the cluster Service. Not applied by CI.
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: digital-human-conversation
  labels:
    app: digital-human-conversation
spec:
  selector:
    matchLabels:
      app: digital-human-conversation
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
```

- [ ] **Step 2:** In `README.md` endpoints table, add Prometheus row next to health; in config table note `prometheus` exposure; add a short **Monitoring** subsection: same port 8080, scrape path, ServiceMonitor example path, Ingress MUST NOT expose `/actuator/**` publicly.

- [ ] **Step 3:** Do not edit `docs/convoai-client-integration.md`.

- [ ] **Step 4:** Run `mvn spotless:apply` && `mvn verify`.

- [ ] **Step 5:** Commit point: `docs: add prometheus scrape notes and ServiceMonitor example`

---

## Done when

- [ ] `GET /actuator/prometheus` returns 200 with JVM series and `application` tag
- [ ] No management separate port
- [ ] Example ServiceMonitor + README updated
- [ ] `mvn verify` passes
