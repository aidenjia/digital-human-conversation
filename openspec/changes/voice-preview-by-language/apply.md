# Apply: voice-preview-by-language

- **Change:** voice-preview-by-language
- **Schema:** superspec
- **Status:** implementation complete
- **Notes:** Preset preview reads catalog `language` (`zh-cn`/`en-us`); `en*` uses English sample text + Tencent `PrimaryLanguage=2`; otherwise Chinese + `1`. Custom preview stays Chinese. Stale cache (`sample_text` mismatch) UPDATE. Frontend query unchanged. `mvn verify` passed.
