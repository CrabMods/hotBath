# Dirt pattern rendering fix

Some valid dirt seeds could select an unavailable texture and crash while a dirty player was rendered. Texture selection now remains valid while preserving ordinary pattern choices.

Verification recorded on October 15 covers Minecraft 1.20.1 / Forge and 1.21.1 / NeoForge: 16 GameTest seed cases and 16 client scenes, including the failing boundary, ordinary seeds and long-value bounds. The dirt layer renders with the correct pattern. The video uses original game captures cropped and held for comparison.

Tested fixes: [Forge](https://github.com/CrabMods/hotBath/commit/ca7d9e4e9b58354bed57d3d917ac06ec74965cd7), [NeoForge](https://github.com/CrabMods/hotBath/commit/03240c4245662d9a84002c06c0b5b92ddc9af8ed). Both are included in the current maintained branches.

This evidence covers ordinary player rendering. Animation-pack compatibility has its own report in #44.
