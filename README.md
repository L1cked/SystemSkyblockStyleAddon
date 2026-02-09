# SystemSkyblockStyleAddon

SystemSkyblockStyleAddon is a Paper 1.21.8 addon for MayorSystem that applies Skyblock-style perk mechanics when those perks are active.
It consumes the MayorSystem API and listens for perk change events so effects update immediately.

![SystemSkyblockStyleAddon Banner](docs/images/banner.png)
<!-- TODO: Replace with your banner image (recommended size: 1280x320) -->

![Status Badge](https://img.shields.io/badge/status-active-brightgreen)
![Paper Badge](https://img.shields.io/badge/paper-1.21.8-blue)
![Java Badge](https://img.shields.io/badge/java-21-orange)
<!-- TODO: Replace badges with your preferred set and links -->

> **Proprietary / All Rights Reserved**
> No license is granted to use, copy, modify, distribute, or create derivative works from this code.
> For permissions, contact the copyright holder.

---

## At a Glance
- Adds Skyblock-style perk mechanics to a normal survival server
- Reads active perk ids from MayorSystem (no duplicate perk definitions)
- Config-driven mechanics that are not exposed in MayorSystem
- Lightweight, event-driven listeners with cooldowns and caps
- Simple admin commands for reload and debug

---

## Screenshots
![Perk Config](docs/images/perk-config.png)

![Debug Output](docs/images/debug-output.png)

![Drop Bonus](docs/images/drop-bonus.png)

---

## Requirements
- Paper 1.21.8 (API 1.21)
- Java 21
- MayorSystem (required)

---

## Quick Start
1. Drop `MayorSystem` and `SystemSkyblockStyleAddon` jars into `plugins/`.
2. Start the server once to generate configs.
3. Open `plugins/SystemSkyblockStyleAddon/config.yml` and adjust mechanic values if desired.
4. Configure perk display/lore in `plugins/MayorSystem/config.yml` under `perks.sections.skyblock_style`.
5. Elect a mayor and select perks. Mechanics activate immediately.

---

## How It Works (30-second overview)
- MayorSystem owns the perk catalog and term schedule.
- SystemSkyblockStyleAddon listens for perk changes and maintains a live set of active perk ids.
- For each active perk, the addon loads its `sssa` mechanic definition from its own config and arms listeners.
- When perks are cleared or the term ends, mechanics disarm instantly.

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
- MayorSystem
- Paper 1.21.8 (API 1.21)
- Java 21

---

## Manual Test Plan
1. Start server with both plugins.
2. Force-elect a mayor and apply perks via MayorSystem admin tools.
3. Verify the addon logs perk changes and mechanics trigger in-game.
4. Clear or advance the term and verify perks disarm.

---

## Build (for developers)
```
./gradlew build
```
The shaded jar is produced by the Shadow plugin.

---

## License & Copyright
Copyright (c) 2026 Lou Morel (Canada). All rights reserved.

This repository is proprietary. No license, express or implied, is granted to any person or entity.
You may not use, copy, modify, merge, publish, distribute, sublicense, sell, or create derivative works
from any part of this repository without explicit prior written permission from the copyright holder.

Public visibility (including viewing or forking on a code hosting platform) does not grant permission
to use, modify, or distribute this code for any purpose.

No patent, trademark, or other intellectual property rights are granted by this notice.
