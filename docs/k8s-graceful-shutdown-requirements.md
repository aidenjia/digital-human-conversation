# Kubernetes 部署与优雅下线

服务只支持一个后端进程。Agent 归属和 WebRTC 控制票据保存在进程内，Deployment 使用 `replicas: 1` 和 `strategy.type: Recreate`。发布会中断在途会话，客户端需要重新建立会话。

- 后端容器监听 8080。可用 `/actuator/health/readiness` 和 `/actuator/health/liveness` 配置探针。
- 反向代理须转发 `/ws/bailian/realtime-events` 的 WebSocket Upgrade。浏览器使用语音模式时，通过 HTTPS 提供前端。
- 进程收到 SIGTERM 后停止接受新会话，向已连接的控制客户端发送 `server.draining`。客户端收到事件后释放媒体资源并关闭会话。
- WebRTC 控制连接的排空宽限由 `BAILIAN_REALTIME_DRAIN_GRACE_SECONDS` 控制，默认 15 秒。设置 Pod 的 `terminationGracePeriodSeconds` 时，给入口摘流、应用排空和 Spring Boot 优雅关停留足时间。
- 在部署环境中配置云端凭据和 JWT 签发方。运行参数见 [服务端配置](../README.md#服务端配置)。
