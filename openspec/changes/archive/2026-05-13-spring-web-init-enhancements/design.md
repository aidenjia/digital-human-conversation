## Context

仓库为单模块 Spring Boot 3.5.7 + Java 17，已具备 Web、Actuator、Logback 与端口/探针配置。提案要求在 **不引入多模块、不强制引入 OpenAPI/安全框架** 的前提下，补齐典型 API 服务初始化能力并优化文档。

## Goals / Non-Goals

**Goals:**

- 入参校验失败与未捕获异常均返回 **稳定、可解析** 的 HTTP 语义与 body（优先 Problem Details）。
- 通过 **少量配置** 提升 Tomcat/Jackson 在联调与生产中的可预期性（编码、时区、优雅停机）。
- 提供 **根路径** 轻量响应，便于探活与人工确认服务身份。
- README 成为「新人 10 分钟上手 + 知道下一步该装什么」的入口。

**Non-Goals:**

- 不实现完整认证/授权、不接入网关、不上线 CORS 全开放策略（仅在 README 列为后续项）。
- 不引入 `springdoc-openapi`、消息队列、数据库访问层（除非后续单独变更）。

## Decisions

1. **校验与异常**  
   - 选用 `spring-boot-starter-validation` + `@RestControllerAdvice` 集中处理 `MethodArgumentNotValidException`、`ConstraintViolationException` 与兜底 `Exception`。  
   - **理由**：与 Spring MVC 原生集成，无额外文档生成依赖；Problem Details 与 `spring.mvc.problemdetails.enabled=true` 对齐。

2. **Problem Details**  
   - 开启 `spring.mvc.problemdetails.enabled=true`，异常处理器返回 `ProblemDetail` 或兼容结构。  
   - **理由**：Boot 3 一等支持，利于前后端与中间件统一错误模型。

3. **Jackson 时区**  
   - 默认 `spring.jackson.time-zone=UTC`（或 `Asia/Shanghai` 若团队强约束中国时区）。  
   - **理由**：API JSON 时间戳若不写时区易产生跨环境歧义；**选用 UTC** 更利于分布式与日志对齐，README 中说明业务展示层再转本地时区。  
   - **备选**：若产品强要求本地时区，可改为 `Asia/Shanghai` 并在 README 明示。

4. **根路径**  
   - 使用轻量 `@RestController` 或 `RouterFunction` 返回 JSON（`name`, `version` 来自 `project.version` 需通过 `@Value` 或 `BuildProperties`）。  
   - **决策简化**：使用 `spring.application.name` + 固定 `version` 从 `META-INF` 读取需要 `build-info` 或 `MANIFEST.MF`；最小方案为 `@Value("${app.version:0.0.1-SNAPSHOT}")` 在 `application.properties` 定义 `app.version=${project.version}` 不可直接引用 Maven 变量到运行时除非 resource filtering。  
   - **实现**：在 `pom.xml` 的 `<build><resources><resource><filtering>true` 只对特定文件 - 常用 `application.yaml` filtering。更简单：`application.properties` 增加 `app.version=@project.version@` 需要 `pom.xml` resources filtering enabled for application.properties.

I'll enable resource filtering in pom for application.properties:
```xml
<resources>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>
    <includes>
      <include>application.properties</include>
      <include>application-prod.properties</include>
    </includes>
  </resource>
  <resource>
    <directory>src/main/resources</directory>
    <filtering>false</filtering>
    <excludes>
      <exclude>application.properties</exclude>
      <exclude>application-prod.properties</exclude>
    </excludes>
  </resource>
</resources>
```
That's verbose. Simpler: use `spring-boot-maven-plugin` build-info goal or just hardcode version in controller from `@Value("${app.info.version:unknown}")` and set in pom with flatten - simplest for skeleton: **read from Environment's "spring.application.name" + literal "0.0.1-SNAPSHOT" in properties** without filtering:

`app.version=0.0.1-SNAPSHOT` duplicate of pom - bad.

Use Spring Boot's `BuildProperties` - requires:
```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>build-info</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
Then inject `BuildProperties` in controller - good approach.

I'll add build-info execution to pom and a small RootController.

5. **优雅停机与压缩**  
   - `server.shutdown=graceful`，`server.compression.enabled=true`（mime-types 默认即可）。  
   - **权衡**：压缩增加 CPU，默认仅对文本响应明显收益；可在 prod profile 再开——设计为默认开启，README 说明可调。

## Risks / Trade-offs

- **[Risk]** Problem Details 与自定义 body 混用导致客户端解析不一致 → **缓解**：全局 advice 统一返回 `ProblemDetail` 或 `ResponseEntity<ProblemDetail>`。  
- **[Risk]** `UTC` 时区与业务期望本地时间不一致 → **缓解**：README 明确约定；必要时改为 `Asia/Shanghai`。  
- **[Risk]** `build-info` 未生成时启动失败 → **缓解**：`BuildProperties` 可选注入 `ObjectProvider<BuildProperties>` 或使用 `@Autowired(required = false)`。

## Migration Plan

- 首次合并：无数据迁移；部署时仅需沿用既有 `LOG_PATH`、profile 环境变量。  
- 回滚：移除新依赖与类文件、还原 `application.properties` 即可。

## Open Questions

- API 是否统一前缀 `/api/v1`（当前不强制，后续变更可加 `context-path` 或 `WebMvcConfigurer` path prefix）。
