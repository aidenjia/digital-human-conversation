## Why

运维需要用 Grafana/Prometheus 观察本服务可用性与延迟，但当前仅暴露
Actuator `health`/`info`，缺少 Prometheus 指标端点与 scrape 约定，无法接入
现有监控栈。现在补齐最小暴露与仓库内 ServiceMonitor 示例，便于平台侧对接。

## What Changes

**Actuator 指标暴露**
- From: `management.endpoints.web.exposure.include` 仅为 `health,info`
- To: 增加 `prometheus`；指标与业务共用 `server.port`（默认 8080）
- Reason: Prometheus 标准拉取路径
- Impact: non-breaking；新端点，业务 API 不变

**依赖与标签**
- 新增 `micrometer-registry-prometheus`
- 通用标签 `application` = `spring.application.name`；开启 HTTP server requests histogram
- Impact: non-breaking

**部署对接示例**
- 新增 `deploy/k8s/servicemonitor.example.yaml`（示例，非强制 apply）
- README 说明 scrape path、同端口约定、Ingress 勿公网暴露 `/actuator`

**测试**
- 集成测试覆盖 `GET /actuator/prometheus` 返回可解析的 Prometheus 文本

## Capabilities

### New Capabilities
- `prometheus-metrics`: Actuator Prometheus 端点暴露、指标标签约定、与业务同端口及 scrape 文档/示例

### Modified Capabilities
- `docker-container-runtime`: 明确容器仍暴露单一 HTTP 端口；Prometheus 与 health 同端口可达（不强制改 Dockerfile HEALTHCHECK）

## Impact

- **依赖**: `pom.xml` 增加 Micrometer Prometheus registry
- **配置**: `application.yml`（及必要时 prod profile）management 段
- **文档**: `README.md`；可选提及 `docs/` 仅当需要运维专页（默认可只改 README）
- **仓库**: `deploy/k8s/servicemonitor.example.yaml`
- **测试**: `WebApiFoundationIT` 或新建 IT
- **业务 API / ConvoAI 契约**: 无变更；`docs/convoai-client-integration.md` 无需改
- **平台**: 正式 ServiceMonitor apply、Grafana 看板、Ingress 规则由运维落地
