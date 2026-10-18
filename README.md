![100w.png](https://s2.loli.net/2026/02/03/1CustOA9eSNYqEk.png)

# Hot-bath Mod

<!-- PROMO-VIDEO:START -->

**Take a break from your adventures. Build a warm bath, wash away dirt and recharge with six bath fluids, portable bath bottles and useful mod integrations.**

`Forge 1.20.1` · `NeoForge 1.21.1`

[![Watch the trailer](https://i.ytimg.com/vi/4oi4OFmUXuY/maxresdefault.jpg)](https://www.youtube.com/watch?v=4oi4OFmUXuY)

▶ [Watch the trailer](https://www.youtube.com/watch?v=4oi4OFmUXuY) · 0:54 · 1080p · English captions, music and effects

[CurseForge](https://www.curseforge.com/minecraft/mc-mods/hotbath) · [Modrinth](https://modrinth.com/mod/hot-bath) · [GitHub](https://github.com/CrabMods/hotBath)

<!-- PROMO-VIDEO:END -->

[![GitHub](https://img.shields.io/badge/Source%20Code-GitHub-181717?logo=github&logoColor=white)](https://github.com/CrabMods/hotBath)

[![Mod Download](https://img.shields.io/badge/Download-Mod-FFB119?logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/hotbath)

## Introduction

Welcome to **Hot Bath Mod**! Take a break from your adventures and soak in a relaxing hot bath. It's not just about hygiene; it's about buffs, healing, and magical effects! Whether you need to recover health, remove negative effects, or just warm up, we have the perfect bath for you.

## Recommended Mods

- **🛁 Official Extension | [ShowerCore](https://www.curseforge.com/minecraft/mc-mods/showercore) 🛁**
  Ready to take your hygiene to the next level? 🚿✨ Introducing **ShowerCore**, the _official_ HotBath extension that brings the splash! We're talking fully functional **Bathtubs**, refreshing **Showers**, and the absolute necessity—**Rubber Ducks**! 🦆💦 Don't just bathe, _celebrate_ cleanliness!

- **🌟 Highly Recommended | Fluidlogged 🌟**  
  [Modrinth Link](https://modrinth.com/mod/fluidlogged/versions)  
  Fluidlogged enables custom fluid blocks to have a waterlogged effect with blocks like stairs, slabs, and more, allowing hotbath fluids to integrate naturally into various structures.  
  **Version note**: Minecraft 1.20.1 needs Fluidlogged for this. Native Hot Bath waterlogging is 1.21.1+ NeoForge.

## Known Issues

- **Optifine Incompatibility**:  
  Optifine may cause rendering issues with fluid colors in this mod. For example, fluids like milk bath may not display their correct colors and appear watery. This is a known incompatibility with Optifine, which affects the rendering of many mods. We recommend using alternative shader mods such as **Sodium** or **Oculus** for better compatibility.

## Update 4.0 (1.20.1 Forge / 1.21.1 NeoForge)

### Dirtiness System

A brand-new hygiene mechanic that tracks how dirty your character gets over time.

- **Gradual dirt accumulation** over 5 in-game days (base rate), influenced by multiple environmental factors:
  - **Biome**: Nether (1.6x), Badlands/Desert/Swamp (1.5x), Jungle (1.3x), Ocean/River (0.85x)
  - **Activity**: Sprinting (1.3x), Crawling (1.4x), Swimming (0.7x), Flying (0.9x)
  - **Weather**: Rain (0.6x) and Thunderstorms (0.5x) help wash you off
  - **Depth**: Deep caves below Y=0 (1.4x), caves below Y=50 (1.2x)
  - **Ground type**: Dirt/Sand blocks slightly increase accumulation
- **Instant dirt events**: Getting hurt (+2%), killing mobs (+3%), killing bosses (+8%), nearby explosions (+5%), mining (+0.5%)
- **Visual dirt overlay** on player model with 10 procedural patterns, progressively covering legs → body → arms → head
- **Fly particles** spawn around the player after staying at 100% dirtiness for 2+ in-game days
- **Bathing cleans you up**: Standing in any hot bath fluid cleans you in ~20 seconds; moving in the water speeds it up to ~15 seconds
- Debug commands: `/dirtiness get`, `/dirtiness set`, `/dirtiness clean`
- Can be toggled off in config

---

### Datapack-Driven Custom Fluids

Create unlimited custom bath fluids via datapacks — no code or recompilation required.

- **Datapack path**: `data/<namespace>/hotbath/custom_fluids/<name>.json`
- **Fully configurable properties**:
  - Color (RGB), opacity, luminosity (0–15)
  - Temperature (°C), viscosity, density
  - Still and flowing textures (defaults to grayscale tinting)
  - Particle toggles: splash, bubbles, steam
  - Potion effects with configurable duration, amplifier, ambient, and icon visibility
  - Trigger time (seconds before effects apply)
  - Nutrition and thirst values (for compat mod integration)
- **Built-in localization**: Define translations for multiple languages directly in the JSON (`en_us`, `zh_cn`, `ja_jp`, etc.) with locale fallback chains
- **Full item support**: Custom fluid buckets, drinkable bottles, and splash bottles are automatically generated
- **Fluid spreading**: Custom fluids propagate their identity through BlockEntity data when flowing
- **Waterlogging support (1.21.1+ NeoForge)**: Hot Bath fluids can be placed in waterloggable blocks such as stairs and slabs
- **Hot reload**: Fluids update when datapacks are reloaded
- **Public API** (`CustomFluidAPI`): Other mods can query, create, and apply custom fluid effects programmatically
- **Guide book** (Patchouli): Includes documentation, JSON format reference, and example fluids (Golden Bath, Slime Bath, Blazing Bath, Ender Bath, Enchanted Bath, Milk Tea Bath, Silent Spring Bath)

**Example Datapack Downloads:**

- [hotbath_example_fluids_1.21.1.zip](https://github.com/CrabMods/hotBath/releases/download/example-datapacks/hotbath_example_fluids_1.21.1.zip) — For Minecraft 1.21.1 (NeoForge)
- [hotbath_example_fluids_1.20.1.zip](https://github.com/CrabMods/hotBath/releases/download/example-datapacks/hotbath_example_fluids_1.20.1.zip) — For Minecraft 1.20.1 (Forge)

---

### Mod Compatibility

All integrations are optional — Hot Bath detects installed mods at runtime and enables features automatically. If a compat fails, it is disabled gracefully with an in-game notification.

#### Alex's Mobs

- **Dirty player interactions**: Dirty players attract flies, mosquitoes (aggressive), and cockroaches (loitering)
- **Raccoons**: Wash items 1.5x faster in hot bath fluids; increased tame chance when washing items in hot bath fluids
- **Capuchin monkeys**: Attracted to hot springs, receive Regeneration buff, easier to tame (inspired by Japanese macaques)

#### Alex's Caves

- **Gummy Bears** take damage in hot water (melting mechanic)
- **Herbal Bath** can cure the IRRADIATED effect
- **Gammaroach** attracted to dirty players
- **Raycat** sits near hot springs

#### Serene Seasons

- Bathing in winter grants bonus Resistance (Early/Late Winter: Level I, Mid Winter: Level II)
- Ice and snow melt near hot bath fluids

#### Twilight Forest

- Bathing in hot water removes Frost effects
- Firefly particle effects appear around bath pools in the Twilight Forest dimension
- Ice-type mobs (Ice Crystal, Stable/Unstable Ice Core, Snow Guardian, Snow Queen) take damage in hot water

#### Farmer's Delight

- Bathing grants the Comfort effect (regenerates 1 HP every 4 seconds, ignoring hunger level)

#### Patchouli

- In-game Hot Bath Guide book with dynamic page visibility based on which compat mods are active

---

## Update 3.0 (1.20.1 Forge / 1.21.1 NeoForge+)

### New Items: Bath Water Bottles

**Drinkable Bottles:**

- **Hot Water Bottle** ![Hot Water Bottle](https://s2.loli.net/2025/12/07/YB1KqswfznZvxid.png):
  Speed![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) I (45s)
- **Honey Bath Water Bottle** ![Honey Bath Water Bottle](https://s2.loli.net/2025/12/07/34IyHoOUxDT8irj.png):
  Absorption![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) I (45s)
- **Milk Bath Water Bottle** ![Milk Bath Water Bottle](https://s2.loli.net/2025/12/07/lr8Sje1fL9wW3pF.png):
  Randomly remove 1 negative effect
- **Herbal Bath Water Bottle** ![Herbal Bath Water Bottle](https://s2.loli.net/2025/12/07/yfUbnu9gKc5awxC.png):
  Heal 3 hearts![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png) + Resistance![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) I (45s)
- **Peony Bath Water Bottle** ![Peony Bath Water Bottle](https://s2.loli.net/2025/12/07/fyVUswaK6JQnmWj.png):
  Luck![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) I (45s)
- **Rose Bath Water Bottle** ![Rose Bath Water Bottle](https://s2.loli.net/2025/12/07/kSva3K96Q1XJ8VB.png):
  Strength![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) I (45s)

**Splash Bath Water Bottles:**

- **Effect**:
  Thrown like a splash potion. Upon impact, it grants a **Level 2 "Cold Resistance" buff (30s)** ![Cold Resistance](https://s2.loli.net/2025/12/07/FrMAaPkR7gbKj3x.png) to players hit.
- **Hot Water** ![Splash Hot Water Bottle](https://s2.loli.net/2025/12/07/kn4mq9bCjcviISh.png):
  Speed![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) II (30s)
- **Honey** ![Splash Honey Bath Bottle](https://s2.loli.net/2025/12/07/QeagmWRt1pUfckJ.png):
  Absorption![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) II (30s)
- **Milk** ![Splash Milk Bath Bottle](https://s2.loli.net/2025/12/07/SXYvsiyW7IwmTPd.png):
  Remove 2 negative effects
- **Herbal** ![Splash Herbal Bath Bottle](https://s2.loli.net/2025/12/07/e4WsaK1EMH2bUqj.png):
  Resistance![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) II (30s) + Heal 2 hearts![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)
- **Peony** ![Splash Peony Bath Bottle](https://s2.loli.net/2025/12/07/hfmQY3gHIi9onlV.png):
  Luck![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) II (30s)
- **Rose** ![Splash Rose Bath Bottle](https://s2.loli.net/2025/12/07/6nmwBWHXQTJGMYO.png):
  Strength![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) II (30s)

### Splash Bath Water Brewing Recipes

Add **Gunpowder** to any Bath Water Bottle to create its splash version.

![splash_hot_water_bottle_composite_eng.png](https://s2.loli.net/2025/12/11/IEuCqy2rO1x7voK.png)

### Fluid Interactions

- **Bubble Columns**:
  Soul Sand creates upward bubbles, Magma Blocks create downward bubbles.
- **Visuals**:
  Custom bubble and drip particles matching the fluid color.

### Mod Integrations

> **Note**: These three mods all introduce temperature mechanics. To avoid conflicts or redundant features, it is **recommended to install only one** of them at a time.

- **[Tough As Nails](https://www.curseforge.com/minecraft/mc-mods/tough-as-nails)**:
  - Bottles provide warming effect.
  - HotBath fluids provide warming effect.
- **[Cold Sweat](https://www.curseforge.com/minecraft/mc-mods/cold-sweat)**:
  - Bottles increase body temp to ~36°C.
  - HotBath fluids maintain body temp at ~37°C.
- **[Legendary Survival Overhaul](https://www.curseforge.com/minecraft/mc-mods/legendary-survival-overhaul)**:
  - Bottles restore thirst ![Hydration](https://s2.loli.net/2025/12/07/IbFe9QB1EdzwKHM.png) and give "Hot Drink" buff ![Hot Drink](https://s2.loli.net/2025/12/07/4ndgOW6GizqkD72.png).
  - HotBath fluids provide "Cold Resistance" ![Cold Resistance](https://s2.loli.net/2025/12/07/FrMAaPkR7gbKj3x.png) and "Thermostatic" buffs ![Thermostatic](https://s2.loli.net/2025/12/07/sA1TaRtDWKE3ymX.png).

## Classic Features (Items & Recipes)

### Bath Herb![Bath Herb.png](https://s2.loli.net/2023/04/07/IkPTs9oiJXHFWqK.png)

**How to get:**

- Buy 1 bath herb for 10 emeralds![Emerald.png](https://s2.loli.net/2023/04/08/fw4jOyVhkgrmFHX.png) from **Novice Farmer Villagers**.
- Buy 1 bath herb for 15 emeralds![Emerald.png](https://s2.loli.net/2023/04/08/fw4jOyVhkgrmFHX.png) from **Wandering Traders**.

### Important Notes

- **Aquatic Life**: Fish (except tropical fish) and octopuses will take damage in HotBath fluids. Aquatic creatures will not spawn naturally.
- **Placement**: Coral, Conduits, and Sea Grass cannot be placed in HotBath fluids.
- **Fishing**: You cannot catch items by fishing in HotBath fluids.

### The Buckets

#### Hot Water ![Hot Water Bucket.png](https://s2.loli.net/2023/04/07/XYJBf8jmznaL7kr.png)

**Description**: Soaking your feet is really comfortable.

**Recipe**: Water Bucket![Water Bucket.png](https://s2.loli.net/2023/04/08/mTFAS1uIUkDybCQ.png) + Smelting (Any Fuel)

**Effects**:

- Bathing for 15s gives **Speed![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) I** (20s).
- **Hidden Achievement [Foot Health]**: Bathe 100 times. Reward: 100 XP.

#### Milk Bath ![Milk Bath Bucket.png](https://s2.loli.net/2023/04/07/k4sI8SHvE3pfrFj.png)

**Description**: It's not for drinking.

**Recipe**: Hot Water Bucket + Milk Bucket![Milk Bucket.png](https://s2.loli.net/2023/04/08/CH4uJGBlXftIY7S.png)

![milk_bath_bucket composite.png](https://s2.loli.net/2023/04/14/QjiAKvuzX4tgnfE.png)

**Effects**:

- Restores **Health![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2s) and **Hunger![Hunger.png](https://s2.loli.net/2023/04/07/Muq5dLpmibwOkWs.png)** (1/15s).
- Bathing for 15s removes all negative effects (except Bad Luck![Bad Luck.png](https://s2.loli.net/2023/04/08/unxXs4IqG8SP5Dk.png)).
- **Hidden Achievement [Milk Skin]**: Bathe 100 times. Reward: 100 XP.

#### Herbal Bath ![Herbal Bath Bucket.png](https://s2.loli.net/2023/04/07/XvrICsoYZy9MfHj.png)

**Description**: Bah, why is this so bitter?

**Recipe**: Hot Water Bucket + Bath Herb x2

![bath_herb_composite.png](https://s2.loli.net/2023/04/14/N4xX7IgsL9vwhaH.png)

**Effects**:

- Restores **Health![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2s).
- Bathing for 15s removes **all** negative effects.
- Bathing for 5s gives **Resistance![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) I** (10s).
- Damages Undead mobs (0.5/s).
- **Hidden Achievement [Chronic Invalid]**: Bathe 100 times. Reward: 100 XP.

#### Peony Bath ![Peony Bath Bucket.png](https://s2.loli.net/2023/04/07/OFyVon7BhYZPfcT.png)

**Description**: It would be better if there was a beauty.

**Recipe**: Hot Water Bucket + Peony x2![Peony.png](https://s2.loli.net/2023/04/08/6yvZNRKkDOaw17m.png)

![peony_bath_bucket composite.png](https://s2.loli.net/2023/04/14/lcuf1i5Lbde3a2G.png)

**Effects**:

- Restores **Health![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2s).
- Bathing for 15s removes negative effects and Bad Omen![Bad Omen.png](https://s2.loli.net/2023/04/08/XFiLbo7GpABESZr.png).
- Bathing for 15s gives **Knockback Resistance** (+0.05, 30s) and **Attack Speed** (+10%, 15s).
- **Special**: After 50 baths, every subsequent bath gives **Luck![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) I** (45s).

#### Honey Bath ![Honey Bath Bucket.png](https://s2.loli.net/2023/04/07/mEzaoJX1YADjHfd.png)

**Description**: This bath is a little bit sticky.

**Recipe**: Hot Water Bucket + Honeycomb Block![Honeycomb Block.png](https://s2.loli.net/2023/04/08/S3BRdFJHV9UoNsq.png)

![honey_bath_bucket composite.png](https://s2.loli.net/2023/04/14/jOzVXciDkxS8dH5.png)

**Effects**:

- Restores **Health![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/s) and **Hunger![Hunger.png](https://s2.loli.net/2023/04/07/Muq5dLpmibwOkWs.png)** (1/4s).
- Gives **Slowness![Slowness.png](https://s2.loli.net/2023/04/08/2EOkSc63WRDIwng.png) I** (10s) while bathing.
- Bathing for 15s removes negative effects (except Slowness/Bad Luck).
- Bathing for 15s gives **Absorption![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) II** (20s).

#### Rose Bath ![Rose Bath Bucket.png](https://s2.loli.net/2023/04/07/7kDylN8VWsMeLzx.png)

**Description**: What a romantic treat.

**Recipe**: Hot Water Bucket + Rose Bush x8![Rose Bush.png](https://s2.loli.net/2023/04/08/vCiNFBXp8HWV47n.png)

![rose_bath_bucket composite.png](https://s2.loli.net/2023/04/14/NczwiMtxAUjXekO.png)

**Effects**:

- Restores **Health![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/s).
- Bathing for 15s removes negative effects and Bad Omen![Bad Omen.png](https://s2.loli.net/2023/04/08/XFiLbo7GpABESZr.png).
- Bathing for 15s gives **Strength![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) I** (20s).
- **Hidden Achievement [Rose Body Fragrance]**: Bathe 100 times. Reward: 100 XP.

---

# Hot-bath Mod (中文介绍)

## 简介

欢迎来到 **Hot Bath Mod (热浴模组)**！在紧张的冒险之余，来泡个舒服的热水澡吧。这不仅仅是为了清洁，更是为了获得增益、治疗和神奇的效果！无论你是需要恢复生命、移除负面效果，还是仅仅想暖暖身子，我们都有适合你的浴汤。

## 推荐模组

- **🛁 官方扩展 | [洗浴核心 (ShowerCore)](https://www.curseforge.com/minecraft/mc-mods/showercore) 🛁**
  准备好让你的洗浴体验焕然一新了吗？🚿✨ 隆重推出 **洗浴核心 (ShowerCore)** —— HotBath 的 _官方_ 延伸模组，带你嗨翻浴室！这里有功能齐全的 **浴缸**、清爽无比的 **淋浴**，还有绝对不能少的灵魂伴侣——**小黄鸭**！🦆💦 别只是洗澡，要 _享受_ 洗澡！

- **🌟 强烈推荐 | Fluidlogged 🌟**  
  [Modrinth 链接](https://modrinth.com/mod/fluidlogged/versions)  
  Fluidlogged 可以让自定义流体方块（如本模组的浴水）与楼梯、台阶等方块共存（含水效果），让你的浴室设计更加自然美观。  
  **版本说明**：Minecraft 1.20.1 需要 Fluidlogged；Hot Bath 原生 waterlogging 支持 1.21.1+ NeoForge。

## 已知问题

- **不支持 Optifine**:  
  Optifine 可能会导致本模组中的流体颜色渲染出现问题，例如牛奶浴等流体可能不会显示正确的颜色，呈现出水样效果。这是 Optifine 与许多模组不兼容的已知问题。我们建议使用 **Sodium (钠)** 或 **Oculus** 等替代光影模组以获得更好的兼容性。

## 4.0 重大更新 (1.20.1 Forge / 1.21.1 NeoForge)

### 脏污度系统

全新的卫生系统，追踪你的角色随时间变脏的程度。

- **渐进式脏污积累**，基础速率为 5 个游戏日达到最脏，受多种环境因素影响：
  - **生物群系**: 下界 (1.6x)、恶地/沙漠/沼泽 (1.5x)、丛林 (1.3x)、海洋/河流 (0.85x)
  - **活动状态**: 疾跑 (1.3x)、匍匐 (1.4x)、游泳 (0.7x)、飞行 (0.9x)
  - **天气**: 雨天 (0.6x) 和雷暴 (0.5x) 能帮助冲洗
  - **深度**: Y=0 以下的深层洞穴 (1.4x)、Y=50 以下的洞穴 (1.2x)
  - **地面类型**: 泥土/沙子方块会略微增加积累速度
- **瞬时脏污事件**: 受伤 (+2%)、击杀生物 (+3%)、击杀 Boss (+8%)、附近爆炸 (+5%)、挖矿 (+0.5%)
- **视觉污垢覆盖层**: 玩家模型上有 10 种程序化污垢图案，按腿部 → 躯干 → 手臂 → 头部渐进显示
- **苍蝇粒子**: 100% 脏污度持续 2 个以上游戏日后，苍蝇会在玩家周围生成
- **泡澡清洁**: 站在任何温泉液体中约 20 秒即可完全清洁；在水中移动可加速至约 15 秒
- 调试命令：`/dirtiness get`、`/dirtiness set`、`/dirtiness clean`
- 可在配置中关闭

---

### 数据包驱动的自定义液体

通过数据包创建无限自定义浴液 — 无需编程或重新编译。

- **数据包路径**: `data/<namespace>/hotbath/custom_fluids/<name>.json`
- **完全可配置的属性**:
  - 颜色 (RGB)、透明度、亮度 (0–15)
  - 温度 (°C)、粘度、密度
  - 静止和流动纹理（默认灰度着色）
  - 粒子开关: 水花、气泡、蒸汽
  - 药水效果（可配置持续时间、等级、环境效果和图标可见性）
  - 触发时间（效果生效前所需秒数）
  - 营养值和口渴值（用于兼容模组联动）
- **内置本地化**: 直接在 JSON 中定义多语言翻译（`en_us`、`zh_cn`、`ja_jp` 等），支持语言回退链
- **完整物品支持**: 自动生成自定义液体桶、饮用瓶和喷溅瓶
- **流体传播**: 自定义液体通过 BlockEntity 数据在流动时传播其身份标识
- **含水支持（1.21.1+ NeoForge）**: Hot Bath 流体可放入楼梯、台阶等可含水方块
- **热重载**: 数据包重载时液体即时更新
- **公共 API** (`CustomFluidAPI`): 其他模组可以通过编程查询、创建和应用自定义液体效果
- **指南书** (Patchouli): 包含文档、JSON 格式参考和示例液体（黄金浴、史莱姆浴、烈焰浴、末影浴、附魔浴、奶茶浴、寂静之泉浴）

**示例数据包下载:**

- [hotbath_example_fluids_1.21.1.zip](https://github.com/CrabMods/hotBath/releases/download/example-datapacks/hotbath_example_fluids_1.21.1.zip) — 适用于 Minecraft 1.21.1 (NeoForge)
- [hotbath_example_fluids_1.20.1.zip](https://github.com/CrabMods/hotBath/releases/download/example-datapacks/hotbath_example_fluids_1.20.1.zip) — 适用于 Minecraft 1.20.1 (Forge)

---

### 模组兼容

所有联动均为可选 — Hot Bath 在运行时自动检测已安装的模组并启用对应功能。若联动失败，会优雅地禁用并在游戏内通知玩家。

#### Alex's Mobs

- **脏污玩家互动**: 脏污的玩家会吸引苍蝇、蚊子（攻击性）和蟑螂（徘徊）
- **浣熊**: 在温泉液体中洗物品速度提升 1.5 倍；在温泉液体中洗物品时更容易驯化
- **卷尾猴**: 被温泉吸引，获得再生 buff，更容易驯化（灵感来自日本猕猴）

#### Alex's Caves

- **软糖熊**在热水中受到伤害（融化机制）
- **草药浴**可以治愈辐射效果
- **伽马蟑螂**被脏污的玩家吸引
- **辐射猫**在温泉旁坐下

#### Serene Seasons（四季）

- 冬季泡澡获得额外抗性提升（初冬/晚冬: I 级，仲冬: II 级）
- 温泉附近的冰雪会融化

#### Twilight Forest（暮色森林）

- 泡热水澡移除冰冻效果
- 暮色森林维度的浴池周围出现萤火虫粒子效果
- 冰系怪物（冰晶、稳定/不稳定冰核、雪卫兵、雪女王）在热水中受到伤害

#### Farmer's Delight（农夫乐事）

- 泡澡获得舒适（Comfort）效果（每 4 秒恢复 1 HP，无视饥饿值）

#### Patchouli

- 游戏内 Hot Bath 指南书，根据已启用的兼容模组动态显示/隐藏对应页面

---

## 3.0 版本更新 (1.20.1 Forge / 1.21.1 NeoForge+)

### 新物品：浴水瓶

**饮用型浴水瓶:**

- **热水瓶** ![Hot Water Bottle](https://s2.loli.net/2025/12/07/YB1KqswfznZvxid.png):
  速度![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) I (45 秒)
- **蜂蜜浴水瓶** ![Honey Bath Water Bottle](https://s2.loli.net/2025/12/07/34IyHoOUxDT8irj.png):
  伤害吸收![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) I (45 秒)
- **牛奶浴水瓶** ![Milk Bath Water Bottle](https://s2.loli.net/2025/12/07/lr8Sje1fL9wW3pF.png):
  随机移除 1 个负面效果
- **草药浴水瓶** ![Herbal Bath Water Bottle](https://s2.loli.net/2025/12/07/yfUbnu9gKc5awxC.png):
  恢复 3 心生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png) + 抗性提升![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) I (45 秒)
- **牡丹浴水瓶** ![Peony Bath Water Bottle](https://s2.loli.net/2025/12/07/fyVUswaK6JQnmWj.png):
  幸运![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) I (45 秒)
- **玫瑰浴水瓶** ![Rose Bath Water Bottle](https://s2.loli.net/2025/12/07/kSva3K96Q1XJ8VB.png):
  力量![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) I (45 秒)

**喷溅型浴水瓶:**

- **效果**:
  像喷溅药水一样投掷。撞击后，给予被击中的玩家 **2 级“抗寒”buff (30 秒)** ![Cold Resistance](https://s2.loli.net/2025/12/07/FrMAaPkR7gbKj3x.png)。
- **热水** ![Splash Hot Water Bottle](https://s2.loli.net/2025/12/07/kn4mq9bCjcviISh.png):
  速度![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) II (30 秒)
- **蜂蜜** ![Splash Honey Bath Bottle](https://s2.loli.net/2025/12/07/QeagmWRt1pUfckJ.png):
  伤害吸收![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) II (30 秒)
- **牛奶** ![Splash Milk Bath Bottle](https://s2.loli.net/2025/12/07/SXYvsiyW7IwmTPd.png):
  移除 2 个负面效果
- **草药** ![Splash Herbal Bath Bottle](https://s2.loli.net/2025/12/07/e4WsaK1EMH2bUqj.png):
  抗性提升![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) II (30 秒) + 恢复 2 心生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)
- **牡丹** ![Splash Peony Bath Bottle](https://s2.loli.net/2025/12/07/hfmQY3gHIi9onlV.png):
  幸运![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) II (30 秒)
- **玫瑰** ![Splash Rose Bath Bottle](https://s2.loli.net/2025/12/07/6nmwBWHXQTJGMYO.png):
  力量![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) II (30 秒)

### 喷溅药水酿造配方

所有种类的浴水瓶加入 **火药** 即可酿造为喷溅型浴水瓶。

![splash_hot_water_bottle_composite_chn.png](https://s2.loli.net/2025/12/11/FBXqQyTa6C9iM5I.png)

### 流体交互

- **气泡柱**:
  灵魂沙产生上升气泡，岩浆块产生下沉气泡。
- **视觉效果**:
  自定义气泡和滴水粒子，颜色与流体一致。

### 模组联动

> **注意**: 这三个模组都引入了温度机制。为了避免冲突或功能冗余，**建议每次只安装其中一个**。

- **[Tough As Nails](https://www.curseforge.com/minecraft/mc-mods/tough-as-nails)**:
  - 浴水瓶提供升温效果。
  - HotBath 流体提供升温效果。
- **[Cold Sweat](https://www.curseforge.com/minecraft/mc-mods/cold-sweat)**:
  - 浴水瓶将体温升至约 36°C。
  - HotBath 流体将体温维持在约 37°C。
- **[Legendary Survival Overhaul](https://www.curseforge.com/minecraft/mc-mods/legendary-survival-overhaul)**:
  - 浴水瓶恢复口渴值 ![Hydration](https://s2.loli.net/2025/12/07/IbFe9QB1EdzwKHM.png) 并给予“热饮”buff ![Hot Drink](https://s2.loli.net/2025/12/07/4ndgOW6GizqkD72.png)。
  - HotBath 流体提供“抗寒” ![Cold Resistance](https://s2.loli.net/2025/12/07/FrMAaPkR7gbKj3x.png) 和“恒温”buff ![Thermostatic](https://s2.loli.net/2025/12/07/sA1TaRtDWKE3ymX.png)。

## 经典功能 (物品与配方)

### 洗浴用药草 (Bath Herb)![Bath Herb.png](https://s2.loli.net/2023/04/07/IkPTs9oiJXHFWqK.png)

**获取方式：**

- 在**新手级农民村民**处用 10 个绿宝石![Emerald.png](https://s2.loli.net/2023/04/08/fw4jOyVhkgrmFHX.png)购买。
- 在**流浪商人**处用 15 个绿宝石![Emerald.png](https://s2.loli.net/2023/04/08/fw4jOyVhkgrmFHX.png)购买。

### 注意事项

- **水生生物**: 热带鱼以外的鱼类和章鱼在 HotBath 方块中会受到伤害。水中生物不会自然生成。
- **放置限制**: 珊瑚、潮涌核心、海草等无法放置在 HotBath 方块中。
- **钓鱼**: 在 HotBath 方块中无法钓起任何物品。

### 各种浴桶

#### 热水桶 (Hot Water Bucket)![Hot Water Bucket.png](https://s2.loli.net/2023/04/07/XYJBf8jmznaL7kr.png)

**描述**: 泡脚好舒服。

**制作**: 水桶![Water Bucket.png](https://s2.loli.net/2023/04/08/mTFAS1uIUkDybCQ.png) + 任意燃料烧炼

**功效**:

- 浸泡 15 秒获得 **速度![Speed.png](https://s2.loli.net/2023/04/08/DpA421zYdlVkoPr.png) I** (20 秒)。
- **隐藏进度 [足力健]**: 浸泡 100 次。奖励: 100 经验。

#### 牛奶浴桶 (Milk Bath Bucket)![Milk Bath Bucket.png](https://s2.loli.net/2023/04/07/k4sI8SHvE3pfrFj.png)

**描述**: 这不是用来喝的。

**制作**: 热水桶 + 牛奶桶![Milk Bucket.png](https://s2.loli.net/2023/04/08/CH4uJGBlXftIY7S.png)

![milk_bath_bucket composite.png](https://s2.loli.net/2023/04/14/QjiAKvuzX4tgnfE.png)

**功效**:

- 恢复 **生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2 秒) 和 **饥饿值![Hunger.png](https://s2.loli.net/2023/04/07/Muq5dLpmibwOkWs.png)** (1/15 秒)。
- 浸泡 15 秒移除除霉运外的所有负面效果。
- **隐藏进度 [牛奶肌]**: 浸泡 100 次。奖励: 100 经验。

#### 中草药浴桶 (Herbal Bath Bucket)![Herbal Bath Bucket.png](https://s2.loli.net/2023/04/07/XvrICsoYZy9MfHj.png)

**描述**: 呸呸，好苦啊。

**制作**: 热水桶 + 洗浴用药草 x2

![bath_herb_composite.png](https://s2.loli.net/2023/04/14/N4xX7IgsL9vwhaH.png)

**功效**:

- 恢复 **生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2 秒)。
- 浸泡 15 秒移除**所有**负面效果。
- 浸泡 5 秒获得 **抗性提升![Resistance.png](https://s2.loli.net/2023/04/08/NSvw8udpAltyPM4.png) I** (10 秒)。
- 对亡灵生物造成伤害 (0.5/秒)。
- **隐藏进度 [药罐子]**: 浸泡 100 次。奖励: 100 经验。

#### 牡丹浴桶 (Peony Bath Bucket)![Peony Bath Bucket.png](https://s2.loli.net/2023/04/07/OFyVon7BhYZPfcT.png)

**描述**: 浴桶已备好，美人何处觅。

**制作**: 热水桶 + 牡丹 x2![Peony.png](https://s2.loli.net/2023/04/08/6yvZNRKkDOaw17m.png)

![peony_bath_bucket composite.png](https://s2.loli.net/2023/04/14/lcuf1i5Lbde3a2G.png)

**功效**:

- 恢复 **生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/2 秒)。
- 浸泡 15 秒移除负面效果和不祥之兆![Bad Omen.png](https://s2.loli.net/2023/04/08/XFiLbo7GpABESZr.png)。
- 浸泡 15 秒获得 **击退抗性** (+0.05, 30 秒) 和 **攻击速度** (+10%, 15 秒)。
- **特殊**: 累计浸泡 50 次后，每次浸泡获得 **幸运![Luck.png](https://s2.loli.net/2023/04/08/9vfrMjAsBULpX5E.png) I** (45 秒)。

#### 蜂蜜浴桶 (Honey Bath Bucket)![Honey Bath Bucket.png](https://s2.loli.net/2023/04/07/mEzaoJX1YADjHfd.png)

**描述**: 这个汤浴有点粘。

**制作**: 热水桶 + 蜜脾块![Honeycomb Block.png](https://s2.loli.net/2023/04/08/S3BRdFJHV9UoNsq.png)

![honey_bath_bucket composite.png](https://s2.loli.net/2023/04/14/jOzVXciDkxS8dH5.png)

**功效**:

- 恢复 **生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/秒) 和 **饥饿值![Hunger.png](https://s2.loli.net/2023/04/07/Muq5dLpmibwOkWs.png)** (1/4 秒)。
- 浸泡时获得 **缓慢![Slowness.png](https://s2.loli.net/2023/04/08/2EOkSc63WRDIwng.png) I** (10 秒)。
- 浸泡 15 秒移除负面效果 (除缓慢/霉运)。
- 浸泡 15 秒获得 **伤害吸收![Absorption.png](https://s2.loli.net/2023/04/08/ZMf6zspPLeu3DXO.png) II** (20 秒)。

#### 玫瑰浴桶 (Rose Bath Bucket)![Rose Bath Bucket.png](https://s2.loli.net/2023/04/07/7kDylN8VWsMeLzx.png)

**描述**: 浪漫的享受。

**制作**: 热水桶 + 玫瑰丛 x8![Rose Bush.png](https://s2.loli.net/2023/04/08/vCiNFBXp8HWV47n.png)

![rose_bath_bucket composite.png](https://s2.loli.net/2023/04/14/NczwiMtxAUjXekO.png)

**功效**:

- 恢复 **生命值![Health.png](https://s2.loli.net/2023/04/08/sPVrSKa8mWkHRUF.png)** (0.25/秒)。
- 浸泡 15 秒移除负面效果和不祥之兆![Bad Omen.png](https://s2.loli.net/2023/04/08/XFiLbo7GpABESZr.png)。
- 浸泡 15 秒获得 **力量![Strength.png](https://s2.loli.net/2023/04/08/CgmeXzsn8TVFbN9.png) I** (20 秒)。
- **隐藏进度 [玫瑰体香]**: 浸泡 100 次。奖励: 100 经验。

---

---
