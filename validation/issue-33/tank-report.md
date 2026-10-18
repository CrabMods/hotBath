# Create tank bucket interaction

In Survival, regular Create fluid tanks do not accept or dispense fluid directly from a handheld bucket. This is Create's normal behavior. The bucket can pour into the surrounding world instead. Use an Item Drain, Basin or Spout as part of your fluid system.

In Creative, the tested water, lava, six built-in bath and seven custom bath buckets each fill a tank with exactly 1,000 mB of the correct fluid. Custom bath identity is preserved. Water/lava comparisons without Hot Bath show the same Survival/Creative difference.

Verified combinations:

- Minecraft 1.20.1, Forge 47.4.16, Create 6.0.8.
- Minecraft 1.21.1, NeoForge 21.1.215, Create 6.0.9.

Hot Bath checks use the current maintenance branches: [Forge tested code](https://github.com/CrabMods/hotBath/tree/c9513f821ddb0e44454b6cf1e6fd5ed6e8510d9f) and [NeoForge tested code](https://github.com/CrabMods/hotBath/tree/c1b35da17d5a9f78848626959b3c56e9204166a3).

All 34 GameTest cases and 42 client checks pass. The video uses original game frames held for comparison. No Hot Bath code change is needed for this tank behavior.

Create's own fluid-tank guide describes the Survival restriction and recommends Basins, Item Drains and Spouts: [Create guide text](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/generated/resources/assets/create/lang/en_us.json).

Moving-bath or vehicle-mod behavior needs its own exact mod versions, simple reproduction steps and latest.log. These tank checks do not establish support for every vehicle mod or modpack.
