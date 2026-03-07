# SystemSkyblockStyleAddon

SystemSkyblockStyleAddon is a [Paper](https://papermc.io/) 1.21.8 addon for [MayorSystem](https://github.com/L1cked/MayorSystem) that applies Skyblock-style perk mechanics when those perks are active.
It consumes the [MayorSystem](https://github.com/L1cked/MayorSystem) API and listens for perk change events so effects update immediately.

![SystemSkyblockStyleAddon Banner](docs/images/banner.png)
<!-- TODO: Replace with your banner image (recommended size: 1280x320) -->

![Status Badge](https://img.shields.io/badge/status-active-brightgreen)
![Paper Badge](https://img.shields.io/badge/paper-1.21.8-blue)
![Java Badge](https://img.shields.io/badge/java-21-orange)
<!-- TODO: Replace badges with your preferred set and links -->

**Version:** 1.0.0

> **MIT License**
> This project is licensed under the MIT License.
> See `LICENSE` for full terms.

---

## At a Glance
- Adds Skyblock-style perk mechanics to a normal survival server
- Reads active perk ids from [MayorSystem](https://github.com/L1cked/MayorSystem)
- Perk display data is synced into [MayorSystem](https://github.com/L1cked/MayorSystem) config on startup for menus
- Config-driven mechanics stay in this addon and are not exposed in [MayorSystem](https://github.com/L1cked/MayorSystem)
- Lightweight, event-driven listeners with cooldowns and caps
- Simple admin commands for reload and debug

---

## How It Works (30-second overview)
- [MayorSystem](https://github.com/L1cked/MayorSystem) owns the perk catalog and term schedule.
- SystemSkyblockStyleAddon listens for perk changes and maintains a live set of active perk ids.
- For each active perk, the addon loads its `sssa` mechanic definition from its own config and arms listeners.
- When perks are cleared or the term ends, mechanics disarm instantly.
- On startup, [MayorSystem](https://github.com/L1cked/MayorSystem) imports perk display metadata from this addon into its own config for menus.

---

## Included Perks
- `prospector_week` (block drop bonus)
- `smelters_decree` (auto smelt)
- `harvest_yield` (crop bonus)
- `replant_blessing` (auto replant)
- `mob_bounty_program` (mob drop bonus)
- `xp_dividend` (exp multiplier)
- `fishers_market` (fishing bonus)
- `builders_amnesty` (fall protection)
- `anvil_tax_cut` (anvil discount)

---

## Requirements
- [Paper](https://papermc.io/) 1.21.8 (API 1.21)
- [Java 21](https://adoptium.net/temurin/releases/?version=21)
- [MayorSystem](https://github.com/L1cked/MayorSystem) (required)

---

## Quick Start
1. Drop [MayorSystem](https://github.com/L1cked/MayorSystem) and `SystemSkyblockStyleAddon` jars into `plugins/`.
2. Start the server once to generate configs.
3. Open `plugins/SystemSkyblockStyleAddon/config.yml` and adjust mechanic values if desired.
4. On first start, [MayorSystem](https://github.com/L1cked/MayorSystem) imports perk display data into `plugins/MayorSystem/config.yml` under `perks.sections.skyblock_style`.
   You can edit it there afterward, or delete the section to re-sync from the addon.
5. Elect a mayor and select perks. Mechanics activate immediately.

---

## Commands
```
/sssa reload
/sssa debug [player]
```

---

## Permissions
| Node | Default | Description |
| --- | --- | --- |
| `sssa.admin` | op | Access `/sssa` admin commands |

---

## Configuration Highlights
- `debug_logging`: Enables extra console logging for perk sync and mechanics.
- `perks.<perkId>.sssa`: Mechanic definition for each perk.
- `type`: Mechanic type (BLOCK_DROP_BONUS, AUTO_SMELT, CROP_BONUS, AUTO_REPLANT, MOB_DROP_BONUS, EXP_MULTIPLIER, FISHING_BONUS, ANVIL_DISCOUNT, FALL_PROTECT).

---

## Configuration Examples
- [Example config.yml](docs/examples/config.yml)

---

## Dependencies & Integrations

### Required
- [MayorSystem](https://github.com/L1cked/MayorSystem)
- [Paper](https://papermc.io/) 1.21.8 (API 1.21)
- [Java 21](https://adoptium.net/temurin/releases/?version=21)

---

## Manual Test Plan
1. Start server with both plugins.
2. Force-elect a mayor and apply perks via [MayorSystem](https://github.com/L1cked/MayorSystem) admin tools.
3. Verify the addon logs perk changes and mechanics trigger in-game.
4. Clear or advance the term and verify perks disarm.

---

## Build (for developers)
```
./gradlew clean jar
```
This produces the thin upload jar in `build/libs/` (no shaded dependencies).
Runtime dependencies are downloaded by [Paper](https://papermc.io/)/[Spigot](https://www.spigotmc.org/) from `plugin.yml -> libraries` at server startup.

Optional local fat jar (not for upload):
```
./gradlew shadowJar
```

---

## License
This project is licensed under the MIT License.

Copyright (c) 2026 Lou Morel (Canada)

See `LICENSE` for the full text.
