# Proposal: cleanup-unused-deps-config

## What

清理 ConvoAI 网关代码库中**未使用的 Maven 依赖**与**无效/误导性配置**，使 `pom.xml` 与 `application*.yml` 与当前产品范围（声网 ConvoAI join/leave）一致。

## Why

- TRTC 云 API、Redis 转录上下文、UserSig 等能力已移除，但 `pom.xml` 仍声明 Tencent SDK、tls-sig、Redisson 等，增加构建体积与安全审计噪声。
- `application-dev.yml` / `application-test.yml` 含 `springdotenv` 配置，但项目无对应依赖。
- `application.yml` 配置 `org.springframework.ai` 日志级别，但无 Spring AI 依赖。
- README 仍引用已删除的 `logback-spring.xml`。

## Scope

**In scope**

- 移除未引用依赖：`tls-sig-api-v2`、`tencentcloud-sdk-java-common`、`tencentcloud-sdk-java-trtc`、`commons-codec`、`redisson`
- 清理 YAML：`springdotenv`、`org.springframework.ai`、错误 logger 包名
- 删除 `pom.xml` 中已注释的死代码块
- 更新 `README.md` 技术栈描述

**Out of scope**

- 重命名 `trtc` 包（历史包名，另有 refactor change）
- 归档 `openspec/specs/trtc-*` 历史规格
- 移除 Jasypt（local profile 仍依赖 `ENC(...)`）

## Success Criteria

- `mvn verify` 通过
- `pom.xml` 仅保留 ConvoAI 网关实际使用的运行时依赖
- 无新增对外 API 行为变更
