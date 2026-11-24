# Verification checklist

- [x] Retained HTTP mapping allowlist and no relational datasource gate pass.
- [x] Agora join defaults, timezone, warm, lock and leave ownership tests pass.
- [x] Obsolete PCM handler/upstream client are absent; WebRTC commands, greeting, text, interrupt, heartbeat and drain tests pass.
- [x] UI has only Agora/WebRTC; removed runtime imports, endpoints and PCM audio helpers are absent.
- [x] Current docs/specs reflect retained behavior; archives and local Avatar assets are preserved.
- [x] Clean Maven verify, frontend tests, lint, build, dependency and diff checks pass.
- [x] Record separately whether real authenticated Agora/WebRTC calls and target-machine visual checks were performed.

Evidence: `docs/voice-only-cleanup-verification.md`. Real authenticated provider calls and target-machine motion visual acceptance were not performed; local Chromium navigation passed.
