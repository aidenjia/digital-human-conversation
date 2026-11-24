## Context

服务已是 Spring Boot 3.5 + Actuator（`health`/`info`，probes 已开），Docker 暴露
`8080`，业务鉴权只覆盖 `/api/convoai/**`。监控栈期望 Prometheus 拉取 + Grafana
展示。探索结论：同端口暴露 `/actuator/prometheus`，ServiceMonitor 示例进仓库；
外网靠 Ingress/NetworkPolicy 隔离。

## Goals / Non-Goals

**Goals:**
- 应用可被 Prometheus scrape（文本格式 `/actuator/prometheus`）
- 低基数标签与 HTTP 延迟直方图，便于 Grafana 基础看板
- 仓库提供 ServiceMonitor 示例与 README 对接说明
- `mvn verify` 覆盖端点可用性

**Non-Goals:**
- 独立 `management.server.port`
- 自定义 join/leave 业务指标、OTel Tracing、Loki 改造
- 在本仓库维护生产级 Grafana dashboard JSON / Alertmanager 规则
- 为 Actuator 增加应用内 Bearer/basic auth（一期靠网络隔离）

## Decisions

1. **同端口 8080**  
   不引入 management 独立端口，避免改 Docker/探针拓扑。Prometheus 经 ClusterIP
   scrape；公网 Ingress 只应转发业务 path。

2. **仅白名单暴露 `health,info,prometheus`**  
   禁止 `include: '*'`，避免 `env`/`beans`/`heapdump` 泄露。

3. **Micrometer 自动装配**  
   只加 `micrometer-registry-prometheus`，不手写 registry Bean；不新增业务包。

4. **标签与基数**  
   `management.metrics.tags.application` = `spring.application.name`；
   `env` 按 profile 默认（`local`/`dev`/`test`/`prod`），可用 `METRICS_ENV` 覆盖。
   禁止会话级 ID 作为 metric label。

5. **HTTP histogram**  
   `management.metrics.distribution.percentiles-histogram.http.server.requests: true`
   以支持延迟分位/热力（接受指标序列略增）。

6. **ServiceMonitor 示例路径**  
   `deploy/k8s/servicemonitor.example.yaml`：标注 example、需按集群 label/namespace
   修改；不作为 CI 强制 apply。

7. **显式开启 Prometheus export**  
   设置 `management.prometheus.metrics.export.enabled: true`。环境中若存在
   `management.defaults.metrics.export.enabled=false`，仅靠 exposure.include
   不会装配 `PrometheusMeterRegistry`，`/actuator/prometheus` 会 404。

8. **文档边界**  
   README 写清端点与安全约定；客户端对接文档不改。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `/actuator/prometheus` 被公网扫到 | README 明确；Ingress 不转发 `/actuator`；可选 NetworkPolicy |
| Histogram 增加时序基数 | 仅对 http.server.requests 开启；后续可再收紧 |
| 示例 YAML 与平台 CRD/label 不一致 | 文件名含 `.example`；注释写明需对齐 |
| 误以为示例会自动部署 | README 声明「示例，由平台 GitOps 落地」 |

## Migration Plan

1. 合并依赖与配置后滚动发布；旧版本无 prometheus 端点，新版本新增，兼容。
2. 平台创建/更新 ServiceMonitor 指向新端点后，Grafana 导入 JVM/HTTP 看板。
3. 回滚：回退镜像即可；去掉 ServiceMonitor 不影响业务流量。

## Open Questions

无。平台正式 label 命名以示例注释为准，实施时与 SRE 对齐一次即可。
