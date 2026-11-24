## Design Summary

为 ConvoAI HTTP 网关接入 Grafana/Prometheus：应用通过 Spring Boot Actuator + Micrometer
暴露 `/actuator/prometheus`（与业务同端口 `8080`）；仓库提供 ServiceMonitor 示例供平台
对齐 scrape；外网隔离由 Ingress/NetworkPolicy 负责（不走独立 management 端口）。
一期不做自定义业务指标与 Tracing。

## Alternatives Considered

### Alternative A: 同端口 + Micrometer Prometheus（选定）
- **Approach**: 增加 `micrometer-registry-prometheus`；`management.endpoints` 暴露
  `health,info,prometheus`；指标与业务共用 `server.port=8080`；仓库内放
  `servicemonitor.example.yaml`；README 说明勿公网暴露 `/actuator`。
- **Pros**: 改动最小；与现有 Docker/探针一致；Boot 自动装配；平台可直接按 path scrape。
- **Cons**: 依赖外围（Ingress/NetworkPolicy）挡外网访问 metrics。
- **Why chosen**: 用户明确选择同端口；一期目标是打通 scrape → Grafana，加固可二期。

### Alternative B: 独立 management 端口
- **Approach**: `management.server.port=8081`，业务 Ingress 只绑 8080，Prometheus 只刮 8081。
- **Pros**: 业务面与指标面端口隔离更清晰。
- **Cons**: 多端口/Service/探针配置；Docker EXPOSE 与 K8s 清单都要改。
- **Why not chosen**: 用户选择同端口；隔离用 Ingress path 控制即可满足一期。

### Alternative C: OpenTelemetry Collector 推送
- **Approach**: OTel Java agent / Micrometer→OTLP，由 Collector 转 Prometheus 远端写。
- **Pros**: 与 tracing 统一管道，利于多后端。
- **Cons**: 运维与依赖更重；超出「先接 Grafana/Prometheus」范围。
- **Why not chosen**: YAGNI；仓库尚无 OTel 基础设施约定。

## Agreed Approach

采用 **Alternative A**：同端口暴露 Prometheus 文本指标 + 仓库内 ServiceMonitor 示例 +
文档约定外网不暴露 Actuator metrics。标签仅低基数（`application`，可选 `env`）；
禁止 `deviceId`/`agentId` 等会话级 label。HTTP server requests 开启 histogram 以支撑延迟看板。
业务自定义 counter（join/leave）列为二期，不在本 change 实现。

## Key Decisions

| ID | Decision |
|----|----------|
| D1 | 同端口 `8080`，不引入 `management.server.port` |
| D2 | 依赖仅加 `micrometer-registry-prometheus` |
| D3 | 暴露白名单：`health,info,prometheus`（不 include `*`） |
| D4 | `management.metrics.tags.application` = `spring.application.name` |
| D5 | 开启 `http.server.requests` percentile histogram |
| D6 | ServiceMonitor **示例**进仓库（如 `deploy/k8s/servicemonitor.example.yaml`） |
| D7 | 外网隔离：Ingress 只放行业务 path；Prometheus 走 ClusterIP scrape |
| D8 | 一期无业务自定义指标；无 OTel/Loki 变更 |
| D9 | 更新 README；集成测试断言 `/actuator/prometheus` |

## Open Questions

无阻塞项。平台侧 Grafana 看板导入与正式 apply ServiceMonitor 属运维落地，本 change
只提供应用能力与示例清单。
