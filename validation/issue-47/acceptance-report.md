# Hot Bath #47 — final acceptance, 2026-10-17

The bucket capability and Spout repair was merged in PR #73 (NeoForge) and #74 (Forge). This pass completes the remaining JEI recipe-viewer acceptance on the current maintenance heads. No production source is changed.

| Lane | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Native Spout filling | 30 transactions passed | 30 transactions passed |
| Native Item Drain block-entity ticks | 29 variants passed | 29 variants passed |
| Visible JEI Filling by Spout recipes | water/lava + six built-in bath buckets, all 8 passed | all 8 passed |
| Patchouli Create entry | visible with Create, unavailable entry without Create | same |
| Actual client scenarios / original captures | 11 / 33 | 11 / 33 |

Create 6.0.8 / Forge 47.4.16 / JEI 15.20.0.106 / Patchouli 1.20.1-84.1-FORGE; Create 6.0.9 / NeoForge 21.1.215 / JEI 19.21.2.313 / Patchouli 1.21.1-92-NEOFORGE. 99 JVM tests and canonical build gates pass. Each accepted runtime uses one process and one world; hidden/unfocused/muted input isolation, configuration restoration, hashes and runtime exit were audited. Gradle may reuse unchanged outputs.

Native Item Drain covers 15 bucket variants (water/lava, six built-in baths, seven example custom IDs) and 14 bottle variants (water, six baths, seven custom IDs). Checks assert exact fluid identity/amount and one returned empty container. Native Spout transactions also retain simulation/invalid-ID controls in the existing regression helper. JEI uses actual visible lookups and the native RecipesGui; no recipe injection or unhiding.

The with-Create GUI lane from attempt D passed before a later tank fixture failed. Its unchanged GUI sources/assets, dependencies, launchers, head and raw frames are retained and bound. Attempt F independently passes native Spout/Item Drain and no-Create checks. D/E are not reported as passing whole suites.

## Explicit remaining scope in #33

Direct tank interaction did not pass the FakePlayer controller fixture: even the vanilla water control returned PASS with no transfer, including after waiting for native block initialization. No root cause or Hot Bath regression is claimed. This needs an actual-client packet reproduction and Create-only control. It is not counted as passed or used to block the independently satisfied #47 acceptance.

Moving contraptions/Aeronautics and regular dedicated-server/multiplayer acceptance remain open. This is not full Create compatibility, a new code patch, or a versioned release. Separate source/evidence self-review passed; it was not an independent review.

Heads: Forge `c9513f821ddb0e44454b6cf1e6fd5ed6e8510d9f`; NeoForge `c1b35da17d5a9f78848626959b3c56e9204166a3`. Video: 61 seconds, held original GUI frames, SHA-256 `f53f2fc54debb56964beabd7e7f4db191e97c79ca2272b6f99f89642dce8fbe4`.
