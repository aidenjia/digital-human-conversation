# Why

The existing VRM avatar needs reusable motion assets. HY-Motion can produce source FBX assets.

# What Changes

Add authenticated job submission, polling, persistent local task records, bounded FBX download caching and a React production panel.

# Capabilities

## New Capabilities
- hy-motion-generation: Generate and retrieve user-owned motion assets.

# Impact

New api.motion, motion and infrastructure.motion packages, opt-in configuration and a panel on the realtime page. Use a server-only TokenHub API Key and the existing OkHttp connection pool; no new dependencies.

# Non-goals

VRM retargeting, automatic conversational gesture selection, runtime generation and deployment are outside this change.
