## Context

此前通过 `WebMvcConfigurer#configurePathMatch` + `HandlerTypePredicate.forBasePackage` 为整个 `api` 包挂 `/api/v1`。现改为类级 `@RequestMapping("/api/v1")`。

## Goals / Non-Goals

**Goals:**

- 删除专用 `WebMvcConfigurer` 实现；对外路径与集成测试 URL 不变。

**Non-Goals:**

- 不引入 `server.servlet.context-path`。
- 不改为网关唯一前缀（仍由应用内映射承担）。

## Decisions

1. **类级 `@RequestMapping("/api/v1")`**：与 Spring 惯例一致，单测与 OpenAPI 生成器均易识别。

## Risks / Trade-offs

- **[Risk]** 新接口漏写前缀 → 通过包约定 + CR；可选后续加 ArchUnit（非本变更）。

## Migration Plan

- 无数据迁移；合并后对外 `POST /api/v1/echo` 不变。

## Open Questions

- 无。
