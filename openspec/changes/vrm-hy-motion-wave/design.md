# Design

The FBX contains one 2.9667 second clip, 90 sampled keys, skin meshes and repeated helper bones. Export 11 named upper-body rotations and discard render assets and translation channels.

A normalized left-arm axis identifies the source-to-target 180 degree Y basis change. Retargeted quaternions address normalized bone UUIDs. Missing chest/shoulder channels compose into available nodes.

Each frame restores a separate procedural base before procedural updates, then applies sampled gesture rotation and transition blends. Gaze, mouth and facial expression remain independent. End, cancellation and repeat use time-based pose transitions.
