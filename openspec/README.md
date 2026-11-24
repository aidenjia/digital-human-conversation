# OpenSpec（Superspec）

本仓库默认 schema：**superspec**（OpenSpec + Superpowers）。

**使用说明（团队必读）：** [docs/openspec-superspec-usage.md](../docs/openspec-superspec-usage.md)

## 快速开始

在 Cursor Agent 中：

```text
/opsx:propose <change-name>    # 创建 change 并生成 artifact
/opsx:apply                    # 按 plan TDD 实现
/opsx:archive                  # 归档并合并 spec
```

配置见 [config.yaml](./config.yaml)。Schema 源码见 [schemas/superspec/](./schemas/superspec/)。
