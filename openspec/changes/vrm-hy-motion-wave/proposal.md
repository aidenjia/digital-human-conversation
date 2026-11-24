# Why

The avatar needs to play the user-generated motion rather than a procedural placeholder.

# What Changes

Extract a compact wave asset, retarget normalized VRM axes, integrate fade/replay/interruption and a stop control.

# Capabilities

## New Capabilities
- vrm-hy-motion-wave: Retarget and play the supplied standing wave.

# Impact

Frontend avatar code, animation asset, preparation tool, tests and documentation. No backend protocol changes.

# Non-goals

Arbitrary FBX upload/retargeting UI, full-body locomotion, finger animation and automatic semantic gesture planning.
