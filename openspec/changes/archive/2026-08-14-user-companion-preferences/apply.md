# Apply: user-companion-preferences

## Implemented

- Persistence entities/mappers for `companion` / `companion_voice` / `user_companion_config`
- `CurrentUserIdAccessor` (JWT `sub`)
- `TencentTtsParamMapper` + join preference merge
- `GET /api/companions`, `GET /api/companion-voices`, `POST /api/user-companion-configs`
- Resource Server authenticated matchers for the new paths
- Join optional `businessCode` + idle default 180 + silence_config
- Local voice (`voiceSource=local` → stored `""`); custom via `voiceBusinessCode`; no default `gentle_female`
- Split GET audio/interaction + client docs

## Verify

```bash
mvn spotless:apply
mvn verify
```
