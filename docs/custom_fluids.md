# HotBath Custom Fluid Extension System

This system allows you to create custom bath fluids using data packs. Custom fluids use a grayscale texture that is tinted at runtime with your specified color, and can apply configurable potion effects to players.

## Quick Start

1. Create a JSON file in your data pack at:

   ```
   data/<your_namespace>/hotbath/custom_fluids/<your_fluid>.json
   ```

2. Define your fluid using the JSON schema below.

3. Reload data packs (`/reload` command) to load your custom fluids.

## JSON Schema

```json
{
  "id": "yourmod:your_bath",
  "color": 16766720,
  "temperature": 40.0,
  "viscosity": 1000,
  "density": 1000,
  "luminosity": 2,
  "show_particles": true,
  "show_bubbles": true,
  "show_steam": true,
  "catalyst_item": "minecraft:gold_ingot",
  "trigger_time_seconds": 15,
  "effects": [
    {
      "effect": "minecraft:regeneration",
      "duration": 200,
      "amplifier": 1,
      "ambient": true,
      "show_particles": false,
      "show_icon": true
    }
  ],
  "texture_still": "hotbath:block/still_custom_fluid_grayscale",
  "texture_flowing": "hotbath:block/flowing_custom_fluid_grayscale"
}
```

## Field Reference

### Required Fields

| Field | Type             | Description                                                  |
| ----- | ---------------- | ------------------------------------------------------------ |
| `id`  | ResourceLocation | Unique identifier for your fluid (e.g., "mymod:golden_bath") |

### Optional Fields

| Field                  | Type             | Default                                      | Description                                           |
| ---------------------- | ---------------- | -------------------------------------------- | ----------------------------------------------------- |
| `name_key`             | String           | "fluid.{namespace}.{path}"                   | Translation key for localization (see Localization)   |
| `color`                | Integer          | 4579817 (cyan)                               | RGB color value (see Color Calculation)               |
| `temperature`          | Float            | 40.0                                         | Temperature in Celsius (visual only)                  |
| `viscosity`            | Integer          | 1000                                         | Flow resistance (higher = slower flow)                |
| `density`              | Integer          | 1000                                         | Fluid density (affects buoyancy)                      |
| `luminosity`           | Integer          | 2                                            | Light level emitted (0-15)                            |
| `show_particles`       | Boolean          | true                                         | Whether to show splash particles                      |
| `show_bubbles`         | Boolean          | true                                         | Whether to show bubble particles                      |
| `show_steam`           | Boolean          | true                                         | Whether to show steam particles                       |
| `catalyst_item`        | ResourceLocation | null                                         | Item used to create this bath (for recipes)           |
| `trigger_time_seconds` | Integer          | 15                                           | Seconds player must stay in bath before effects apply |
| `effects`              | Array            | []                                           | List of potion effects to apply                       |
| `texture_still`        | ResourceLocation | hotbath:block/still_custom_fluid_grayscale   | Still texture                                         |
| `texture_flowing`      | ResourceLocation | hotbath:block/flowing_custom_fluid_grayscale | Flowing texture                                       |

### Effect Entry Fields

| Field            | Type             | Default  | Description                                           |
| ---------------- | ---------------- | -------- | ----------------------------------------------------- |
| `effect`         | ResourceLocation | Required | The potion effect ID (e.g., "minecraft:regeneration") |
| `duration`       | Integer          | 200      | Effect duration in ticks (20 ticks = 1 second)        |
| `amplifier`      | Integer          | 0        | Effect level (0 = Level I, 1 = Level II, etc.)        |
| `ambient`        | Boolean          | false    | Whether effect particles are more translucent         |
| `show_particles` | Boolean          | true     | Whether to show effect particles on player            |
| `show_icon`      | Boolean          | true     | Whether to show effect icon in HUD                    |

## Localization

Custom fluids support localization through the `name_key` field. This allows you to provide translated names for your fluids in different languages.

### How It Works

1. **Default Behavior**: If `name_key` is not specified, the system generates a translation key automatically:

   ```
   fluid.<namespace>.<path>
   ```

   For example, `mymod:golden_bath` becomes `fluid.mymod.golden_bath`.

2. **Custom Key**: You can specify a custom translation key:
   ```json
   {
     "id": "mymod:special_bath",
     "name_key": "item.mymod.my_custom_translation_key",
     ...
   }
   ```

### Adding Translations

Add your translations to the language files in your resource pack:

**English (`assets/<namespace>/lang/en_us.json`):**

```json
{
  "fluid.mymod.golden_bath": "Golden Bath",
  "fluid.mymod.enchanted_bath": "Enchanted Bath"
}
```

**Chinese (`assets/<namespace>/lang/zh_cn.json`):**

```json
{
  "fluid.mymod.golden_bath": "黄金浴",
  "fluid.mymod.enchanted_bath": "附魔浴"
}
```

### Item Names

The translated fluid name is used to construct item names using these patterns:

- **Bucket**: `%s Bucket` / `%s桶`
- **Bottle**: `%s Bottle` / `%s瓶`
- **Splash Bottle**: `Splash %s Bottle` / `喷溅型%s瓶`

For example, if `fluid.hotbath.golden_bath` = "Golden Bath", the bucket will display as "Golden Bath Bucket".

## Color Calculation

Colors are specified as RGB integers. Calculate using:

```
color = (red × 65536) + (green × 256) + blue
```

### Common Colors

| Color        | RGB           | Value    |
| ------------ | ------------- | -------- |
| Gold         | 255, 215, 0   | 16766720 |
| Lapis Blue   | 60, 68, 170   | 3949738  |
| Slime Green  | 113, 195, 93  | 7458653  |
| Blaze Orange | 255, 127, 0   | 16744448 |
| Ender Purple | 21, 21, 117   | 1381717  |
| Pure Red     | 255, 0, 0     | 16711680 |
| Pure Green   | 0, 255, 0     | 65280    |
| Pure Blue    | 0, 0, 255     | 255      |
| White        | 255, 255, 255 | 16777215 |

## Examples

### Golden Regeneration Bath

```json
{
  "id": "hotbath:golden_bath",
  "color": 16766720,
  "temperature": 42.0,
  "luminosity": 6,
  "catalyst_item": "minecraft:gold_ingot",
  "trigger_time_seconds": 10,
  "effects": [
    {
      "effect": "minecraft:regeneration",
      "duration": 400,
      "amplifier": 1
    },
    {
      "effect": "minecraft:absorption",
      "duration": 600,
      "amplifier": 1
    }
  ]
}
```

### Fire Resistance Bath

```json
{
  "id": "mymod:blazing_bath",
  "color": 16744448,
  "temperature": 80.0,
  "luminosity": 12,
  "show_bubbles": false,
  "catalyst_item": "minecraft:blaze_powder",
  "effects": [
    {
      "effect": "minecraft:fire_resistance",
      "duration": 1200,
      "amplifier": 0
    }
  ]
}
```

### Quiet Meditation Bath (No Particles)

```json
{
  "id": "mymod:quiet_bath",
  "color": 8421504,
  "show_particles": false,
  "show_bubbles": false,
  "show_steam": false,
  "trigger_time_seconds": 30,
  "effects": [
    {
      "effect": "minecraft:night_vision",
      "duration": 600,
      "ambient": true,
      "show_particles": false
    }
  ]
}
```

## API Usage (For Mod Developers)

```java
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import net.minecraft.resources.ResourceLocation;

// Check if a fluid exists
boolean exists = CustomFluidAPI.isFluidRegistered(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Get fluid properties
int color = CustomFluidAPI.getFluidColor(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));
float temp = CustomFluidAPI.getFluidTemperature(
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Apply effects to a player
CustomFluidAPI.applyFluidEffects(serverPlayer,
    ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));

// Create a fluid programmatically
CustomFluidDefinition custom = CustomFluidAPI.builder(
        ResourceLocation.fromNamespaceAndPath("mymod", "my_bath"))
    .color(CustomFluidAPI.rgb(255, 100, 50))
    .temperature(45.0f)
    .luminosity(4)
    .effects(List.of(
        CustomFluidAPI.createEffect(
            ResourceLocation.parse("minecraft:haste"),
            400, 1)
    ))
    .build();
```

### Custom FluidStack Identity

Third-party tanks and pipes should preserve the custom fluid id stored on HotBath's shared dynamic fluid:

```java
import net.neoforged.neoforge.fluids.FluidStack;

ResourceLocation fluidId = ResourceLocation.fromNamespaceAndPath("mymod", "my_bath");
FluidStack stack = CustomFluidAPI.createCustomFluidStack(fluidId, 1000);

ResourceLocation storedId = CustomFluidAPI.getCustomFluidId(stack);
CustomFluidAPI.getCustomFluidDefinition(stack).ifPresent(definition -> {
    float temperature = definition.temperature();
});
```

Use `CustomFluidAPI.setCustomFluidId(stack, fluidId)` when copying or reconstructing a dynamic custom fluid stack. Use `CustomFluidAPI.hasSameCustomFluidId(left, right)` when comparing stacks, because different data-pack fluids share the same underlying dynamic fluid type.

### External Bath Container Integration

Other mods should use the public integration surface instead of depending on HotBath internals:

```java
import com.crabmod.hotbath.api.HotBathApi;

if (HotBathApi.isCleansingFluid(storedFluid)) {
    HotBathApi.applyDirtinessCleaning(serverPlayer, storedFluid, isMoving);
}

float dirtiness = HotBathApi.getDirtiness(serverPlayer);
HotBathApi.addDirtiness(serverPlayer, 0.10f);
```

The same contract is also available to data packs and tags through:

- `hotbath:bath_fluids`
- `hotbath:cleansing_fluids`

External bath containers are responsible for validating their own fluid storage, shape, and immersion checks before calling the API.

## Custom Textures

You can provide your own grayscale textures by:

1. Create your textures (should be grayscale for proper tinting)
2. Place them in your resource pack at:
   ```
   assets/<namespace>/textures/block/<your_texture>.png
   ```
3. Reference them in your JSON:
   ```json
   {
     "texture_still": "mymod:block/my_still_texture",
     "texture_flowing": "mymod:block/my_flowing_texture"
   }
   ```

## Tips

1. **Duration**: 20 ticks = 1 second. A duration of 200 = 10 seconds.

2. **Trigger Time**: Set higher trigger times for powerful effects to require players to stay in the bath longer.

3. **Ambient Effects**: Set `ambient: true` for effects that should feel more natural with subtle particles.

4. **Luminosity**: Higher values (up to 15) make the fluid glow more. Use for magical or hot fluids.

5. **Temperature**: Currently visual/informational only, but can be used by other mods for integration.

## Override the built-in baths

The six native bath behaviours are supplied as JSON inside the mod. They load automatically: no additional default data pack is required. The bundled values preserve the existing effects and balance.

To customise one, copy its complete bundled file into your data pack at the same path:

```text
data/hotbath/hotbath/custom_fluids/hot_water_fluid.json
```

The other native filenames are `milk_bath_fluid.json`, `herbal_bath_fluid.json`, `honey_bath_fluid.json`, `peony_bath_fluid.json` and `rose_bath_fluid.json`. Their `id` must match the filename, for example `hotbath:hot_water_fluid`. Use the normal data-pack `pack.mcmeta` format for your Minecraft version (15 for 1.20.1, 48 for 1.21.1).

These exact six IDs override existing behaviour. They retain their registered fluids, blocks, buckets and bottles, and do not create additional custom-fluid items. Other IDs continue to use the custom-fluid format documented above. The whole native file is replaced according to normal data-pack priority; definitions are not merged field by field. Start with the bundled file so that all required sections are present.

Each native definition contains:

| Field | Meaning |
| --- | --- |
| `temperature` | Bath temperature in Celsius, used by immersion temperature queries and hot/cold checks. Existing bottle integrations and other mods' consumed-item tags retain their own rules. |
| `cleans_dirtiness` | Whether immersion in this native fluid cleans dirtiness, including supported stored-fluid containers. |
| `splash_dirtiness_reduction` | Fraction of maximum dirtiness removed by its splash bottle on a hit; default `0.1`. |
| `bath` | Continuous immersion effects and optional healing, hunger, cleansing, entry rewards and special native bonuses. |
| `drink` | Effects, optional instant healing and cleansing from drinking a bottle. |
| `splash` | Effects, optional instant healing and cleansing from a splash bottle. |
| `pipe` | Effects and cleansing from each Create open-pipe delivery; only used when Create is installed. |

The distinct profiles preserve differences between bathing, drinking, splashing and pipe delivery. For example, milk's cleansing rules and herbal instant healing are separate from potion effects. Registry properties such as density, flow, light and textures are unchanged by these native behaviour files.

Every profile has an `effects` array. To replace the hot-water bath's Speed effect, replace `bath.effects` in the copied file, for example:

```json
[
  {
    "effect": "minecraft:haste",
    "duration": 400,
    "amplifier": 1,
    "ambient": false,
    "show_particles": false,
    "show_icon": true,
    "after_ticks": 100,
    "minimum_entries": 0
  }
]
```

This grants Haste II after five seconds of continuous bathing, with a 20-second effect duration. The bath refreshes eligible effects while occupied, as before. An empty array disables that profile's potion effects; other configured mechanics, such as healing, remain independent. Durations and intervals use game ticks: 20 ticks equal one second. Amplifiers start at zero for level I.

Optional bath mechanics are described by their bundled examples:

- `healing` and `hunger`: `amount` and `interval_ticks`.
- `cleansing`: `after_ticks`, maximum `count`, `category` (`harmful` or `non_beneficial`), `selection` (`random` or `first`), and effect-ID lists `exclude` / `include`. Exclusions take precedence; the large bundled count means all matching effects.
- `advancement`: advancement `id` and required entry count `entries`.
- `peony_bonuses`: attack-speed and knockback amounts, trigger time and separate linger times. This section belongs to the peony bath.
- `undead_damage`: damage `amount` and `interval_ticks`.
- `instant_healing`: immediate health points, used by herbal bottles.
- `extinguish`: whether that delivery extinguishes fire; enabled in the default pipe profiles.

Single deliveries (`drink`, `splash`, `pipe`) cannot wait for continuous bathing or entry counts: their effect `after_ticks` and `minimum_entries` must be zero. Continuous healing, hunger, advancement and peony/undead sections belong to `bath`.

Run `/reload` after changing the pack. A malformed native definition rejects the reload with its resource ID and validation error; the previous valid native and custom catalogs stay active. Disabling the overriding pack restores the bundled defaults. Already granted potion effects expire normally; peony modifiers update their amounts and honour the configured exit times.

The server owns the definitions and synchronises them on join and successful reload. Install matching updated mod builds on the server and clients. Native profiles are kept separate from the custom-fluid creative catalog during synchronisation.
