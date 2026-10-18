# Hot Bath

<!-- PROMO-VIDEO:START -->

**Take a break from your adventures. Build a warm bath, wash away dirt and recharge with six bath fluids, portable bath bottles and useful mod integrations.**

`Forge 1.20.1` · `NeoForge 1.21.1`

[![Watch the trailer](https://i.ytimg.com/vi/4oi4OFmUXuY/maxresdefault.jpg)](https://www.youtube.com/watch?v=4oi4OFmUXuY)

▶ [Watch the trailer](https://www.youtube.com/watch?v=4oi4OFmUXuY) · 0:54 · 1080p · English captions, music and effects

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/hotbath) · [Modrinth](https://modrinth.com/mod/hot-bath) · [GitHub](https://github.com/CrabMods/hotBath)

<!-- PROMO-VIDEO:END -->

Hot Bath adds bath fluids, bath bottles, dirtiness, and mod integrations for Minecraft 1.21.1 on NeoForge.

## Compatibility

- **Waterlogging**: On Minecraft 1.21.1+ NeoForge, Hot Bath supports waterloggable blocks like stairs and slabs natively. Fluidlogged is not required on this branch.
- **Epic Fight**: Hot Bath registers its dirtiness overlay as an Epic Fight patched layer, so dirt renders on Epic Fight's animated player model. Third-person combat animations and first-person arms are both supported automatically.
- **Create**: Hot Bath fluids integrate with Create's fluid system. They can move through tanks, pumps, and pipes; open pipes apply bath-style effects; mixers, spouts, emptying recipes, data-pack custom bath fluids, and similar automation features are supported.
- **External bath containers**: Other mods can identify Hot Bath fluids with the `hotbath:bath_fluids` and `hotbath:cleansing_fluids` fluid tags or `com.crabmod.hotbath.api.HotBathApi`, then call the API to apply gradual dirtiness cleaning from their own validated bath containers. `CustomFluidAPI` also exposes FluidStack helpers so third-party tanks can preserve data-pack custom bath fluid ids.

## 兼容信息

- **Waterlogging**：Minecraft 1.21.1+ NeoForge 已原生支持楼梯、台阶等可含水方块，不需要 Fluidlogged。
- **史诗战斗 / Epic Fight**：热水澡会把污渍度覆盖层注册为史诗战斗的 patched layer，让污垢显示在史诗战斗的动画玩家模型上。第三人称战斗动画和第一人称手臂都会自动兼容。
- **机械动力 / Create**：Hot Bath 流体可以接入 Create 的流体系统，可通过储罐、泵和管道传输；开放管道会触发浴液效果；搅拌、灌装、倒出配方、数据包自定义浴液等自动化功能兼容。
