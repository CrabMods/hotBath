# Optional dirtiness effects

There are no dirtiness effects by default. Server owners and data packs may define any number of ranges in `data/<namespace>/hotbath/dirtiness_effects/<name>.json`:

```json
{
  "rules": [
    {"min": 0.5, "max": 0.8, "effects": [
      {"effect": "minecraft:slowness", "amplifier": 0, "duration": 60, "refresh_interval": 40}
    ]},
    {"min": 0.8, "max": 1.0, "effects": [
      {"effect": "minecraft:slowness", "amplifier": 1, "duration": 60, "refresh_interval": 40},
      {"effect": "minecraft:weakness", "amplifier": 0, "duration": 60, "refresh_interval": 40}
    ]}
  ]
}
```

Dirtiness uses 0–1: 0.5 means 50%. Minimum is inclusive; maximum is exclusive, except 1.0 includes fully dirty players. Ranges may overlap and their lists combine. For the same effect, the strongest level wins, then the longest duration.

Durations and refresh intervals use ticks (20 ticks = 1 second). Duration must be 20–72000, refresh interval 20–duration, and amplifier 0–255 (0 means level I). Defaults are duration 60, refresh interval 40, amplifier 0, hidden particles and visible icon. Set `show_particles` and `show_icon` to override the display flags.

The server checks once per second. Effects are refreshed at the configured interval rather than every tick. Leaving a range, disabling the dirtiness system or removing a rule stops refreshing; the granted effect expires normally within its configured duration. This avoids deleting effects supplied by potions or other mods. Stronger existing effects are not replaced. Global compatibility mode also stops new applications.

`/reload` reloads these rules. Invalid files are logged and ignored as a whole; other valid files remain active. Use an empty list to disable a file. These are server rules and use Minecraft's normal effect synchronization; clients do not need the data pack.
