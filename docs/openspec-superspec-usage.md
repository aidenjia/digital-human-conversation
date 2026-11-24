# OpenSpec + Superspec 使用指南

本仓库使用 **[Superspec](https://github.com/danielhanold/superspec)** 作为默认 OpenSpec schema，把 **OpenSpec**（规格治理）与 **Superpowers**（执行纪律）串成一条可追溯链路：

```text
想法 → brainstorm → proposal/specs/tasks/plan → TDD 实现 → 验证 → 归档
```

| 框架 | 职责 |
|------|------|
| **OpenSpec** | 管 **WHAT**：change 目录、delta spec、任务清单、归档 |
| **Superpowers** | 管 **HOW**：brainstorming、writing-plans、TDD、code review、worktree |
| **Superspec** | 用自定义 schema 在 artifact 各阶段自动调用对应 Superpowers skill |

---

## 前置条件

1. **OpenSpec CLI**（本机 1.2.0+）

   ```powershell
   openspec --version
   ```

   未安装见 [OpenSpec 安装说明](https://github.com/Fission-AI/OpenSpec/blob/main/docs/installation.md)。

2. **Cursor** + **Superpowers 插件**（IDE 内已启用 brainstorming / TDD / code-review 等 skills）。

3. **重启 Cursor**（安装或更新 OpenSpec 集成后，slash 命令才稳定）。

---

## 仓库内相关路径

```text
openspec/
  config.yaml              # 默认 schema: superspec，含本项目 context
  schemas/superspec/       # Superspec schema（升级时整体替换，见下文）
  changes/<change-name>/   # 进行中的 change（artifact 产物）
  specs/                   # 归档后的「活」规格（archive 时从 delta 合并）
  specs/README.md          # 能力域索引与命名约定

docs/
  openspec-superspec-usage.md   # 本文
  superpowers/specs|plans/      # Superspec 引入前的设计草稿，仅作历史参考

.cursor/
  commands/opsx-*.md       # /opsx:propose、apply、archive、explore
  skills/openspec-*/       # propose / apply / archive / explore 技能
  rules/                   # 编码规范（project-core、java-api-convoai 等）
```

与历史文档的关系：

- `docs/superpowers/specs/`、`docs/superpowers/plans/`：Superspec 引入**之前**的 TRTC / 重构设计，**勿**再作为新功能入口。
- `openspec/specs/trtc-*`：腾讯云 TRTC 活规格遗留，代码已移除；新 ConvoAI 能力请新建 `convoai-*` capability。
- 新功能从 **`openspec/changes/<name>/`**（superspec）开始，归档后 delta 合并进 `openspec/specs/`。
- 对外契约同步更新 **`README.md`**。

---

## 一次性全局配置（推荐）

当前全局 OpenSpec 默认为 **core** profile，自带 `/opsx:propose`、`/opsx:apply`、`/opsx:archive`、`/opsx:explore`（本仓库 `.cursor/commands/` 已对齐）。

若要使用 Superspec **完整分步流程**（`/opsx:new`、`/opsx:continue`、`/opsx:ff`、`/opsx:verify`），在终端执行一次：

```powershell
openspec config profile
```

选择 **custom**，并勾选全部 workflows：

`propose`, `explore`, `new`, `continue`, `apply`, `ff`, `sync`, `archive`, `bulk-archive`, `verify`

然后在本仓库根目录刷新 Cursor 集成：

```powershell
cd <repository-root>
openspec init --tools cursor --profile custom --force
```

验证 schema：

```powershell
openspec schemas          # 应看到 superspec (project)
openspec schema validate superspec
```

---

## 在 Cursor 里怎么用

在 **Agent 对话**中输入 slash 命令（不是普通终端）。

### 方式 A：一步到位（适合小改动、需求清晰）

```text
/opsx:propose convoai-greeting-config
```

Agent 会创建 `openspec/changes/convoai-greeting-config/`，并按 Superspec 顺序生成 artifact（brainstorm、proposal、specs、tasks、plan 等，以 schema 为准）。

实现阶段：

```text
/opsx:apply
```

完成后验证与归档：

```text
/opsx:verify
/opsx:archive
```

> 若全局仍是 core profile 且无 `/opsx:verify`，可让 Agent 按 change 内 `tasks.md` / `plan.md` 手动验收，再 `/opsx:archive`。

### 方式 B：分步人工把关（适合非平凡功能，推荐）

需已配置 **custom** profile 的全部 workflows：

```text
/opsx:new convoai-interrupt-keywords
/opsx:continue                  # brainstorm（superpowers:brainstorming）
/opsx:continue                  # proposal
/opsx:continue                  # specs（delta）
/opsx:continue                  # tasks
/opsx:continue                  # plan（superpowers:writing-plans）
/opsx:apply                     # worktree + TDD + 子 agent
/opsx:verify                    # 对照 spec/tasks 验收
/opsx:continue                  # finalize（合并分支、推送、PR 收尾）
/opsx:archive                   # delta 合并进 openspec/specs/
```

每一步产物落在 `openspec/changes/<name>/`，可 review 后再 `continue`。

### 方式 C：快进（需求简单、信任 Agent）

```text
/opsx:ff convoai-token-null-fix
/opsx:apply
/opsx:verify
/opsx:archive
```

### 探索阶段（尚未决定要不要做）

```text
/opsx:explore
```

用于调研、对比方案，不创建 change 目录。

### 临时不用 Superspec

单次 change 可指定上游 schema：

```powershell
openspec new change my-simple-fix --schema spec-driven
```

---

## CLI 常用命令

在仓库根目录执行：

| 命令 | 说明 |
|------|------|
| `openspec new change <name>` | 新建 change（默认 `superspec`，见 `openspec/config.yaml`） |
| `openspec status --change <name>` | 查看 artifact 完成度 |
| `openspec status --change <name> --json` | 机器可读状态（Agent 用） |
| `openspec instructions apply --change <name> --json` | 获取 apply 阶段指令 |
| `openspec validate --all --json` | 全项目结构校验 |
| `openspec schemas` | 列出可用 schema |
| `openspec schema validate superspec` | 校验 schema 完整性 |
| `mvn verify` | 本仓库测试门禁（apply / verify 阶段应通过） |

---

## Superspec 阶段与 Superpowers 注入点

```text
brainstorm ──► proposal ──► specs ──► tasks ──► plan ──► apply ──► verify ──► finalize
     │                                              │         │
     └── brainstorming                    writing-plans   TDD + code-review
                                          (apply 内)      (subagent 内自动)
```

| 阶段 | 产出文件（典型） | Superpowers |
|------|------------------|-------------|
| Brainstorm | `brainstorm.md` | `brainstorming` |
| Proposal | `proposal.md` | — |
| Specs | `specs/**/*.md` | — |
| Tasks | `tasks.md` | — |
| Plan | `plan.md` | `writing-plans` |
| Apply | 代码 + `apply.md` | `using-git-worktrees`、`subagent-driven-development` |
| Verify | `verify.md` | OpenSpec verify |
| Finalize | `finalize.md` | schema 内 git 收尾 |
| Archive | change → `changes/archive/` | delta 合并到 `openspec/specs/` |

设计细节见 `openspec/schemas/superspec/INTEGRATION.md`。

**重要**：brainstorm / writing-plans 的默认输出目录是 `docs/superpowers/`；Superspec 的 artifact instruction 会**重定向**到 `openspec/changes/<name>/`，避免与历史 `docs/superpowers/*` 重复。

---

## 本仓库示例：新增 ConvoAI 能力

假设要为 join 增加可配置欢迎语并落 spec：

1. Agent：`/opsx:propose convoai-greeting-message`
2. Review `openspec/changes/convoai-greeting-message/proposal.md` 与 `specs/convoai-join-leave/spec.md`
3. `/opsx:apply` — Agent 在 worktree 按 plan 写测试与实现
4. 本地：`mvn verify`
5. 更新 `README.md` 中 `convoai.greeting-message` 说明（若对外可见）
6. `/opsx:verify` → `/opsx:archive`

归档后 `openspec/specs/convoai-join-leave/spec.md` 成为活规格；`openspec/changes/archive/YYYY-MM-DD-convoai-greeting-message/` 保留历史。

---

## 与 README 的分工

| 文档 | 读者 | 内容 |
|------|------|------|
| `README.md` | 研发 / 运维 | 技术栈、环境变量、接口摘要 |
| `openspec/specs/*/spec.md` | Agent / 评审 | SHALL/MUST、Scenario，可归档、可验证 |
| `openspec/changes/<name>/` | 单次变更 | proposal、delta、tasks、plan |

行为级变更：**先 OpenSpec change → 实现 → 归档 → 再改 README**，避免文档与代码漂移。

---

## 故障排除

| 现象 | 处理 |
|------|------|
| 没有 `/opsx:new` 等命令 | 执行「一次性全局配置」，重启 Cursor |
| change 用了 `spec-driven` 而非 `superspec` | 检查 `openspec/config.yaml` 首行是否为 `schema: superspec` |
| apply 未走 TDD | 确认 Superpowers 插件启用；Agent 需能调用 `subagent-driven-development` |
| `openspec schemas` 无 superspec | 确认存在 `openspec/schemas/superspec/schema.yaml` |
| 与旧 `docs/superpowers/*` 重复 | 新 change 以 `openspec/changes/` 为准；旧文档仅作历史参考 |
| 误改 `trtc-*` spec | 该域已下线；新建 `convoai-*` capability |

---

## 升级 Superspec schema

```powershell
$tmp = Join-Path $env:TEMP "superspec-tmp"
git clone --depth 1 --filter=blob:none --sparse https://github.com/danielhanold/superspec.git $tmp
Set-Location $tmp
git sparse-checkout set openspec/schemas/superspec
Set-Location <repository-root>
Copy-Item -Recurse -Force "$tmp\openspec\schemas\superspec\*" "openspec\schemas\superspec\"
Remove-Item -Recurse -Force $tmp
openspec schema validate superspec
```

---

## 项目状态快照（2026-06-25）

- **OpenSpec CLI**：请本机执行 `openspec --version` 确认 ≥ 1.2.0
- **Schema**：`superspec` v4（见 `openspec/schemas/superspec/schema.yaml`）
- **活规格**：`web-api-foundation`、`api-routing-policy`、`api-error-envelope`、`application-logging`、`docker-container-runtime`；`trtc-*` 为历史遗留
- **待建 capability**：`convoai-join-leave`（建议下一 change 补齐）
- **测试门禁**：`mvn verify`
- **Superpowers**：Cursor 插件；apply 阶段依赖 brainstorming / writing-plans / subagent-driven-development

---

## 参考链接

- [Superspec](https://github.com/danielhanold/superspec)
- [OpenSpec](https://github.com/Fission-AI/OpenSpec)
- [Superpowers](https://github.com/obra/superpowers)
- 本仓库 schema 集成说明：`openspec/schemas/superspec/INTEGRATION.md`
- 能力域索引：`openspec/specs/README.md`
