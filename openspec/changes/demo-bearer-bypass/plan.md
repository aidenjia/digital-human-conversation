# Demo Bearer Bypass Implementation Plan

> **For agentic workers:** Implement task-by-task; checkboxes track progress.

**Goal:** Optional fixed Bearer for ConvoAI join/leave only; UE5 via DB seed.

**Architecture:** Filter injects synthetic Jwt on path+token match; BearerTokenResolver skips JWKS for that request; Service unchanged.

**Tech Stack:** Spring Security OAuth2 Resource Server, Boot 3.5, MockMvc.

## Global Constraints

- Only `/api/convoai/agents`; dual-track real JWT; 401/4010 envelope; no token in logs; prod demo off.

## Tasks

- [ ] Properties + yml + startup validator
- [ ] Filter + resolver + SecurityFilterChain
- [ ] Tests + README
- [ ] spotless + mvn verify
