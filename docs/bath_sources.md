# Bath source conversion

`config/hotbath-common.toml` contains `enableInfiniteBathSources = true`.
The default preserves the existing behavior on both maintained versions.

- With the option enabled, two suitable matching bath sources can replenish a source over solid support or a matching source below.
- With it disabled, no additional bath source is formed automatically. Existing sources remain sources and can still be collected and placed.
- Switching either way does not remove registered fluids, rewrite chunks, replace blocks or change stored custom-fluid IDs. Flowing liquid still follows normal updates and may recede when its supply changes.
- Different custom-fluid IDs cannot combine to replenish a source. Ordinary same-ID custom baths remain supported.
- Vanilla water/lava source-conversion gamerules are unchanged. The bath option controls bath fluids; other mods may also participate in the loader's fluid-source event.

Changing the option uses the common configuration reload behavior. It does not retroactively tick every fluid block; existing flowing areas respond when their normal fluid updates run.
