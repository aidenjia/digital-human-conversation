# Plan: cleanup-unused-deps-config

## 实施顺序

1. **pom.xml** — 删除 5 个未使用依赖 + 注释块（单文件，先改以便后续编译验证）
2. **YAML** — 按 `design.md` 表逐项清理 4 个 profile 文件 + test resources
3. **README.md** — 一行技术栈修正
4. **验证** — `mvn verify`

## 测试策略

- 无新测试；依赖现有 `WebApiFoundationIT`、`ConvoaiServiceTest`、`ConvoaiControllerTest` 作为回归门禁
- 不修改 Jasypt 相关测试（依赖保留）

## 回滚

- 单 commit revert `pom.xml` + YAML + README 即可恢复
