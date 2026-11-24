# TRTC 云端转录启动接口 Redis 上下文存储 — 设计规格

**日期：** 2026-05-28  
**范围：** `/api/v1/trtc/cloud-transcriptions/start` 接口改造  
**状态：** 已评审（brainstorming 确认）

---

## 背景

当前 `GET /api/v1/trtc/cloud-transcriptions/start` 仅根据鉴权 Header 中的 `userId` 调用腾讯云 `CreateCloudTranscription`，返回进房凭据。业务需要在任务创建后将设备与定位信息写入 Redis，供下游（如字幕回调、风控、审计）按 `taskId` 关联查询。

---

## 目标

1. 接口改为 `POST`，接收 `deviceId` 与可选定位信息。
2. TRTC 任务创建成功后，将上下文写入 Redis：`transcriptions:{taskId}`。
3. 使用 **Redisson** 作为 Redis 客户端。
4. Value 包含 `userId`、`roomId`、`deviceId`、可选 `location`（嵌套经纬度类）。
5. TTL **24 小时**（86400 秒），与 UserSig 有效期对齐。

## 非目标

- 不改造 `/api/v1/trtc/ai-conversations/start`（可后续复用相同模式）。
- 不实现 Redis 读取 API、Stop/Describe 转录。
- Redis 写入失败不做 TRTC 任务补偿或回滚。
- 不保留 GET 兼容（breaking change，前端同步改 POST）。

---

## 已确认决策

| 决策项 | 选择 |
|--------|------|
| HTTP 方法 | `POST` + JSON body |
| Redis Key | `transcriptions:{taskId}` |
| Redis Value 格式 | JSON 字符串（Redisson `RBucket`） |
| Redis 客户端 | Redisson |
| Redis database | `1` |
| TTL | 86400 秒（24 小时） |
| Redis 写入失败 | 部分成功：仍返回 200，记录 `warn` 日志 |
| `deviceId` | 必填 |
| 经纬度 | 可选；若传 `location` 则 lat/lng 均必填且范围合法 |
| 经纬度建模 | 独立 Java 类 `GeoLocation` |

---

## API 规格

### 请求

- **方法 / 路径：** `POST /api/v1/trtc/cloud-transcriptions/start`
- **鉴权：** 不变，`X-Api-Token: {clientUserId}|{fixedToken}`

**Body：**

```json
{
  "deviceId": "device-abc-123",
  "location": {
    "latitude": 23.1291,
    "longitude": 113.2644
  }
}
```

无定位时省略 `location` 字段。

### 校验规则

| 字段 | 规则 |
|------|------|
| `deviceId` | `@NotBlank` |
| `location` | 可选；若存在则 `@Valid` |
| `location.latitude` | `@NotNull`，范围 [-90, 90] |
| `location.longitude` | `@NotNull`，范围 [-180, 180] |

校验失败 → HTTP 400，`ApiResponse` 格式与现有全局校验一致；**不调用 TRTC**。

### 响应

不变：`ApiResponse<StartTranscriptionData>`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sdkAppId": 1400000001,
    "userId": "alice",
    "userSig": "...",
    "strRoomId": "...",
    "taskId": "..."
  }
}
```

---

## 数据模型

### GeoLocation

```java
public record GeoLocation(
    Double latitude,   // [-90, 90]
    Double longitude   // [-180, 180]
) {}
```

带 Bean Validation 注解（`@NotNull`、`@DecimalMin`、`@DecimalMax`）。

**包位置建议：** `con.aidenjia.digitalhuman.conversation.trtc`（后续 AI 对话接口可复用）。

### StartTranscriptionRequest

```java
public record StartTranscriptionRequest(
    String deviceId,
    GeoLocation location  // nullable
) {}
```

### TranscriptionContext（Redis Value）

```java
public record TranscriptionContext(
    String userId,
    String roomId,
    String deviceId,
    GeoLocation location  // nullable，序列化时省略 null
) {}
```

**Redis 存储示例（有定位）：**

```json
{
  "userId": "alice",
  "roomId": "b2c3d4e5f6789012345678abcdef012345",
  "deviceId": "device-abc-123",
  "location": {
    "latitude": 23.1291,
    "longitude": 113.2644
  }
}
```

---

## 架构与数据流

```
TrtcCloudTranscriptionController
  POST + @Valid StartTranscriptionRequest
  clientUserId ← TrtcAuthFilter
       ↓
TrtcCloudTranscriptionService.startTranscription(userId, command)
  ① validateTranscriptionConfiguration()
  ② CreateCloudTranscription（腾讯云）
  ③ TranscriptionContextStore.save(taskId, context)  // best-effort
       ↓
返回 StartTranscriptionResult
```

### 新增组件

| 组件 | 职责 |
|------|------|
| `RedisProperties` | `redis.address`、`redis.password`、`redis.database` |
| `RedissonConfig` | 创建 `RedissonClient` Bean |
| `TranscriptionContextStore` | Key 拼装、JSON 序列化、TTL 写入 |
| `StartTranscriptionCommand` | Service 层入参（deviceId + location） |

### 写入策略

1. TRTC 成功 → 构造 `TranscriptionContext` → `TranscriptionContextStore.save`。
2. Redis 异常 → `log.warn("Failed to save transcription context taskId=...", ex)`，**不抛异常**。
3. TRTC 失败 → 抛 `TrtcUpstreamException`，不写 Redis。

---

## 配置

`application.properties` 增加（密码走环境变量，禁止提交明文）：

```properties
redis.address=${REDIS_ADDRESS:redis://127.0.0.1:6379}
redis.password=${REDIS_PASSWORD:}
redis.database=${REDIS_DATABASE:1}
```

`.env.local` / 编排注入示例：

```
REDIS_ADDRESS=redis://your-host:6379
REDIS_PASSWORD=***
REDIS_DATABASE=1
```

### Maven 依赖

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version><!-- pin 与 Spring Boot 3.5 兼容版本 --></version>
</dependency>
```

若 starter 与现有配置风格冲突，可仅用 `redisson` 核心包 + 手动 `RedissonConfig`（实现时二选一，优先 starter）。

---

## 错误处理

| 场景 | HTTP | 行为 |
|------|------|------|
| 鉴权失败 | 401 | 现有 `TrtcAuthFilter` |
| Body 校验失败 | 400 | 现有 `GlobalExceptionHandler` |
| TRTC 配置缺失 | 5xx | `TrtcConfigurationException` |
| TRTC 上游失败 | 5xx | `TrtcUpstreamException` |
| Redis 写入失败 | 200 | 记录 warn，响应正常 |

---

## 测试计划

1. **TrtcCloudTranscriptionIT**
   - `GET` 改为 `POST`，补 JSON body。
   - 成功路径：Mock Service，验证 200 与响应字段。
   - `deviceId` 缺失 → 400。
   - 非法 `location.latitude` → 400。

2. **TrtcCloudTranscriptionService 单测**（可选，Mock TRTC + Store）
   - TRTC 成功后调用 `TranscriptionContextStore.save`。
   - Redis 失败不抛异常，仍返回结果。

3. **TranscriptionContextStore 单测**（可选，Testcontainers Redis 或 Mock Redisson）
   - Key 为 `transcriptions:{taskId}`。
   - TTL 为 86400 秒。

---

## 影响文件（实现参考）

| 文件 | 变更 |
|------|------|
| `pom.xml` | 添加 Redisson 依赖 |
| `application.properties` | Redis 配置项 |
| `TrtcCloudTranscriptionController.java` | GET→POST，`@RequestBody` |
| `TrtcCloudTranscriptionService.java` | 接收 command，调用 Store |
| `TrtcCloudTranscriptionIT.java` | 适配 POST + body |
| 新增 `GeoLocation.java` | 经纬度 record |
| 新增 `StartTranscriptionRequest.java` | API 请求 DTO |
| 新增 `StartTranscriptionCommand.java` | Service 命令对象 |
| 新增 `TranscriptionContext.java` | Redis value |
| 新增 `TranscriptionContextStore.java` | Redis 写入 |
| 新增 `RedisProperties.java` | 配置绑定 |
| 新增 `RedissonConfig.java` | Redisson 客户端 |

---

## 风险

- **Breaking change：** 前端须同步改为 POST；文档与 OpenAPI（若有）需更新。
- **Redis 不可用：** 转录仍可用，但下游无法按 taskId 查上下文（可接受，已选部分成功）。
- **Redisson 与 Spring Boot 版本：** 实现时 pin 兼容版本并跑全量 `mvn verify`。
