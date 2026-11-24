## Apply

Implemented demo Bearer bypass for ConvoAI agents only.

### Done
- `ApiSecurityProperties` + startup validator
- `DemoBearerAuthenticationFilter` + `DemoAwareBearerTokenResolver`
- Wired in `ResourceServerSecurityConfig`
- yml: base/local defaults enabled with plaintext token; prod forced off; dev also set
- `DemoBearerAuthTest`, `ApiSecurityStartupValidatorTest`
- README demo section + seed SQL

### Verify
- Targeted auth tests passed (`DemoBearerAuthTest`, `JwtResourceServerAuthTest`, etc.)
- Full `mvn verify` recommended before merge
