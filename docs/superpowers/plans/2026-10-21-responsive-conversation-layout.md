# Responsive Conversation Layout Implementation Plan

> **For agentic workers:** Execute inline using executing-plans; preserve the dirty feature branch. No delegation, automatic merge or unrelated commits.

**Goal:** 实时页面宽屏约 6:4 分栏，窄屏上下排列，放大人物与对话区。

**Architecture:** ConversationRealtime 增加稳定双面板结构，专用 CSS 作用域控制实时页，字幕滚动只操作字幕容器。媒体 hooks、模型、动作代码不变。

**Tech Stack:** React / TypeScript / CSS Grid + Flexbox / Chrome geometry checks.

## Global Constraints

- 不修改旧版 Conversation、登录和模式选择页面。
- 不改变人设、模型、输入方式、冷却或音频协议。
- 断点 1024px；桌面使用动态视口高度并提供 vh 回退；低高度允许面板内部滚动。
- 浏览器实测几何与溢出，不用 CSS 字符串断言代替视觉验收。

### Task 1: Scope and red checks

- [x] 在 Chrome 宽屏记录旧布局失败证据：实际视口 1745×982，页面仍为 800px、人物仍为 640px。
- [x] 新建 `conversation-ui/tests/caption-scroll.test.ts`，验证空容器安全、只改变传入容器的 scrollTop；运行并观察缺少实现导致失败。

### Task 2: Structure and scrolling

Files: `conversation-ui/src/ConversationRealtime.tsx`, `conversation-ui/src/services/captionScroll.ts`。

- [x] 新增 helper `scrollCaptionsToEnd(panel: Pick<HTMLElement, 'scrollTop' | 'scrollHeight'> | null): void`，执行 `if (panel) panel.scrollTop = panel.scrollHeight`。
- [x] 将人物放入 realtime-avatar-panel，将原配置/字幕和 control-section 放入 realtime-conversation-panel；外层 workspace 使用稳定双列，不改变 AvatarStage key 或 hooks。
- [x] effect 改为 `scrollCaptionsToEnd(captionPanelRef.current)`，移除 scrollIntoView，避免滚动输入框和整页。
- [x] 重跑字幕滚动测试。

### Task 3: Scoped responsive CSS

File: `conversation-ui/src/ConversationRealtime.css`，由实时页显式导入。

- [x] `.realtime-app .app-container` 取消 max-width，100% 可用高度；workspace 使用 `grid-template-columns: minmax(0, 3fr) minmax(0, 2fr)`。
- [x] 人物面板和 canvas 使用 flex 填充；会话面板内部字幕可滚动、control-section 不收缩；页头和工具栏允许换行。
- [x] `@media (max-width: 1023px)` 改为上下布局、动态画布高度、有界会话高度和页面纵向滚动；所有面板 min-width 为 0。
- [x] 在 Chrome 检查宽屏、笔记本、平板、手机和低高度，记录实际 CSS 视口与矩形；恢复视口覆盖。

### Task 4: Verification and handoff

- [x] `node --test tests/*.test.ts`、`npm run lint`、`npm run build` 通过。
- [x] 真实文字问答验证字幕滚动、输入区可见、人物控件布局；结束测试会话。
- [x] 更新 `docs/responsive-conversation-layout.md` 验收记录，保持功能代码未自动提交，刷新页面生效，无需后端重启。
