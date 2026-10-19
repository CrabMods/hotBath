# Bath benefits for friendly mobs

Non-hostile mobs receive the same applicable immersion effects, healing and cleansing as players, with the bath's existing wait times. This includes animals, villagers, golems and normally classified companion mobs. Custom data-pack fluids also apply their configured effects.

Food restoration and advancements remain player-only. Existing thermal and undead damage rules are retained. Peony bonuses only use attributes the creature actually has, and expire after leaving the bath. Drinking and splash behavior is unchanged.

The default classification excludes hostile monsters and mobs currently targeting a player. Data packs can add unusual companion types to the `hotbath:bath_beneficiaries` entity-type tag, or opt types out with `hotbath:excluded_bath_beneficiaries`. Exclusion takes precedence. Both tags are empty by default. Use `tags/entity_type/` on 1.21.1 and `tags/entity_types/` on 1.20.1.

`HotBathApi.canReceiveBathBenefits(LivingEntity)` exposes the classification. There are no hard dependencies or separate hardcoded integrations for individual NPC mods. A mod's private stamina system requires its own effect consumer; this feature does not invent a stamina API. Compatibility mode disables the added mob benefits. Immersion and exit timers use weak entity keys and add no saved entity data.

Native NeoForge waterlogged baths also support eligible mobs. Forge retains its existing optional Fluidlogged boundary.
