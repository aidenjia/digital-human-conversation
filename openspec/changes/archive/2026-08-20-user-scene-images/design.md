## Context

`digital-human-conversation` 是 ConvoAI HTTP 网关（CD 多副本、无粘滞会话），已有 Postgres + Redis。陪伴配置按 `(user_id, companion_id)` upsert；次元配置按 `user_id` 一行。音色已支持 `local` / 云端必选。场景图尚未建模：无 COS 依赖、无场景表、配置页无法上传/列表/选用。

共享库约定：**不使用 DB 外键**；本仓无 Flyway。资产先例是 `companion_custom_voice`（用户级 + URI）。Postgres 关键词核查：`name` 为 unreserved，`url` 非关键词，可作列名。

## Goals / Non-Goals

**Goals:**

- 新建 `user_scene_image`：用户场景图资产（必填 `name`、COS `object_key`/`url`）
- 按 `user_id` + `scope`（`companion` | `dimension`）共享图库，**不绑角色**
- 本服务代理上传 COS；列表；硬删
- 现有陪伴/次元 upsert 必选场景：`sceneSource=local` 或 `cloud` + `sceneImageId`
- 受保护路径加上 `/api/user-scene-images/**`
- README / 客户端对接文档同步

**Non-Goals:**

- 数量上限、软删、缩略图、平台预设场景库、前端直传、私有桶预签名读
- Flyway；DB 外键
- ConvoAI join 注入场景 URL（客户端用列表/回显的 `url` 自己展示）
- 陪伴与次元共用同一图库（scope 分开）

## Decisions

### 1. 行式资产表，不用 JSONB 存列表

列表/删/选用会并发更新。JSONB 塞进 config 行会导致读改写竞态。`metadata jsonb` 仅存宽高、原文件名等扩展。

相对「两套独立场景表」：本期字段相同，用 `scope` 即可。

### 2. 图库不绑角色

`user_scene_image` 无 `companion_id`。陪伴所有角色共用 companion scope 图库；次元用 dimension scope。各 `user_companion_config` 仍可选用不同图（或都 local）。对齐 `companion_custom_voice`：资产归用户，配置行决定当前用哪张。

删除时：同一事务将所有引用该 id 的 config 回退 `scene_source=''` + `selected_scene_image_id=NULL`，再删行与 COS。

### 3. 选用对齐音色 `local`，并入 upsert，不允许空

| API | 落库 `scene_source` | `selected_scene_image_id` |
|-----|---------------------|---------------------------|
| `sceneSource=local` | `''` | `NULL` |
| `sceneSource=cloud` + `sceneImageId` | `'cloud'` | 该 id |
| 未传 source 但有 id | 按 cloud | 该 id |
| 两者皆无 / local 同时带 id | 400 | 不写库 |

DB CHECK：`scene_source IN ('', 'cloud')`；local 必须 id 为空，cloud 必须 id 非空。代码再校验：图存在、`user_id` 匹配、`scope` 匹配。

旧客户端未传场景字段 → **破坏性 400**（与音色必选一致）。

### 4. 上传与 URL

`POST /api/user-scene-images` multipart：`file`、`name`、`scope`。服务端校验 MIME/大小 → 上传 COS → 写库。失败不写库。

`url` = COS 私有 GET 签名 URL（TTL 1 天），**上传时签发并落库**；列表读取时解析 URL 内 `q-sign-time` 判断是否过期，过期或距失效不足 5 分钟则续期并 `update url`。前端直接访问，本服务不代理读。Key：`{env}/scene/{userId}/{scope}/{snowflake}.{ext}`（env=`local`/`dev`/`test`/`prod`）。

配置：`CosProperties` + 环境变量（secret 不进 Git）；**启动时必须配齐**，由 `CosClientConfig` 创建 `COSClient` Bean。新增腾讯云 COS SDK 依赖（固定版本，禁止 LATEST）。

### 5. HTTP 与包

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/user-scene-images` | POST | 上传 |
| `/api/user-scene-images?scope=` | GET | 列表（含 `used`） |
| `/api/user-scene-images/{id}` | DELETE | 硬删 |

包：`api.scene`（Controller/DTO）→ `scene`（编排）→ `infrastructure.cos` + persistence。陪伴/次元 Service 只解析 `sceneImageId`，不调 COS SDK。

列表 `used`：陪伴对照最近一次启动保存行；次元对照唯一配置行；local 时全部 `used=false`。

### 6. 校验与日志

- `name` trim 后 1–50 字
- 允许 `image/jpeg`、`image/png`、`image/webp`；大小上限配置化（默认 10MiB）
- 日志：禁止输出 COS secret；可记 `scope`、`objectKey`、`byteSize`、`httpStatus`

## Risks / Trade-offs

- [Risk] 共享库未建表/列导致启动失败 → Mitigation：tasks 含 DDL；测试 schema 同步
- [Risk] 旧客户端 upsert 未传场景 → Mitigation：spec/README 标明破坏性必选
- [Risk] COS 与 DB 不一致（上传成功写库失败 / 删库后 COS 失败） → Mitigation：上传先 COS 成功再 insert；删除先清引用再删行，COS 失败 WARN 可运维扫孤儿
- [Risk] 公有读 URL 可被猜测 → Mitigation：key 含 userId + 雪花；禁止桶 list；CORS 限制 Origin
- [Risk] 多副本无粘滞 → Mitigation：无本地磁盘暂存；multipart 在请求线程内直传 COS

## Migration Plan

1. 共享 Postgres：建 `user_scene_image`；两张 config 加 `scene_source`（默认 `''`）与可空 `selected_scene_image_id` + CHECK
2. CD 注入 COS 环境变量；发 conversation
3. 客户端：上传/列表/删除；upsert 必带 `sceneSource`/`sceneImageId`
4. Rollback：回退镜像；列可留（旧行 `scene_source=''` 视为 local）；COS 对象可残留

## Open Questions

- 无。
