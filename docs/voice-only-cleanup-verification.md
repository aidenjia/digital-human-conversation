# 语音链路精简验收记录

> 本文是删除 PCM/数据库等能力时的历史验收快照。随后已移除外部缓存与分布式锁依赖、改为单进程内存状态；当前部署方式以 README 为准。

执行范围：只保留声网 ConvoAI/RTC 与百炼 WebRTC；源代码清理不涉及线上数据库、云资源或外部部署仓库。

## 保留契约

- `POST /api/convoai/agents`、`DELETE /api/convoai/agents`。
- `POST /api/rtc/credentials`。
- `POST /api/bailian/realtime/webrtc`。
- `WS /ws/bailian/realtime-events`，只传控制事件，音频走 WebRTC RTP。
- 根探活、Actuator、JWT、统一信封、Redis 归属/锁/票据与 WebRTC Avatar 播放。

## 自动验证

已提交后端的干净快照（`bfdab5a`，不含用户未提交的 Avatar 改动）执行 `mvn -B -ntp clean verify`：169 项单元测试（1 项 live 探针按配置跳过）与 9 项集成测试通过。当前工作区同一命令：172 项单元测试（1 跳过）与 9 项集成测试通过。

前端 `node --test tests/*.test.ts` 75/75 通过，`npm run lint` 与 `npm run build` 成功。提交快照 `23a7b28` 的前端独立执行 lint/build 也通过，证明提交不依赖用户未提交的动作代码。Vite 保留大于 500kB bundle 的现有警告。

浏览器（Playwright/Chromium，本地 Vite）检查 4 种旧/空模式状态、两张选择卡、进入 WebRTC、切换文字/语音、人设/音色/模型控件、返回模式、声网三种类型选择与退出登录：全部通过，无页面错误或已删除接口请求。此检查使用假 token 且阻断外部网络，没有启动真实通话。

`mvn dependency:tree` 无 PostgreSQL、MyBatis、H2、Freemarker、腾讯 VRS/TTS SDK 或 COS SDK，保留 Agora、Redisson 和 Lock4j。真实带凭据通话和目标机器的动作视觉效果不由编译与单元测试替代。

最终整体审查的 3 个小问题已在 `14e56c9` 修正并复审通过：关停测试消除计时竞争、前端测试还原 sessionStorage、旧端点注释纠正。修正后重新执行完整 `clean verify`（172 项单元测试，1 跳过；9 项集成测试）、75 项前端测试、lint/build，全部通过。保留 Mockito/JDK agent 与 Vite 大包警告。

生产源代码、前端运行时代码和当前对接文档的范围化扫描中，无已删除业务路由、关系数据库依赖或旧 PCM 端点引用。保留的 Lock4j 不属于 MyBatis；历史计划/归档和“拒绝旧能力”的回归断言不作为残余运行逻辑。`git diff --check` 通过；归档规格未改。

## 用户改动保护

执行前已将 42 个原有改动/未跟踪文件和 tracked diff 备份到 `<local-path>`。删除旧 PCM 文件之前保留了其用户改动；其余 Avatar、动作素材和准备工具继续留在工作区。默认 VRM 仍为本地忽略文件，不入库。

欢迎语回归测试和 WebRTC hook 与清理范围有重叠：只迁移删除接口造成的依赖，保留用户行为断言与动作回执；提交时区分任务修改与用户尚未提交的动作协议修改。

备份哈希对照确认：7 个未重叠的后端 Avatar 文件、26 个未重叠的前端 Avatar/动作文件均与执行前一致。重叠 hook 的剩余 diff 仅为用户原有动作回执修改；欢迎语测试的剩余 diff 仅为原有动作意图协议修改。VRM 存在、命中忽略规则且未被 Git 跟踪。

## 交接边界

旧业务源码、SQL/代码生成器与旧前端页面可从 Git 历史恢复；执行前未提交的旧文件内容在上述备份中。没有执行 DROP、清空 Redis、删除云端音色/对象，未修改 `digital-human-agent` 或 `digital-human-cd`。

外部部署需移除旧数据库、COS、VRS、试听与在线动作生成配置。Redis 仍是完整声网归属/leave 的依赖；WebRTC 内存票据只适合单实例。现有 lifecycle phase 顺序和 3s/10s shutdown budget 未改成新的策略，其与默认 15s drain 的限制见优雅关停文档。
