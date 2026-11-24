# Implementation plan

1. Use TokenHub Bearer authentication, snake_case requests and the existing API response conventions.
2. Keep HTTP DTO, application command and infrastructure I/O separate.
3. Persist UUID task ownership in a configurable non-public asset directory.
4. Add authenticated, bounded, redirect-free COS download caching.
5. Add a React production panel with polling and task restoration.
6. Test cloud contract, validation, ownership, status, cache, expiry and download restrictions.
