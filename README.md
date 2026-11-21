# Fletching Works

**English** | [简体中文](README.zh-CN.md)

> The fletching table has been decoration since 1.14. This mod gives it the one job its name promises.

Right-click a fletching table to open a three-slot crafting UI: **tip + stick + feather**. Most recipes yield 4 arrows (TNT 1, redstone 3, pearl / web / fire / frost / wind charge / grapple 2, bee 1, netherite 16). On top of that, **15 arrow types** — from stone and copper to explosive TNT arrows, redstone-pulsing arrows, and a pearl arrow that teleports *you*.

[![Release](https://img.shields.io/github/v/release/ztqjntm/Fletching-Works?style=flat-square&label=release)](https://github.com/ztqjntm/Fletching-Works/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-3C8527?style=flat-square)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBB69B?style=flat-square)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MPL--2.0-blue?style=flat-square)](LICENSE)
[![build](https://github.com/ztqjntm/Fletching-Works/actions/workflows/build.yml/badge.svg)](https://github.com/ztqjntm/Fletching-Works/actions/workflows/build.yml)

<!--
  Banner (optional but recommended): a 1280×400 banner.png at docs/images/banner.png.
  Uncomment the line below once it exists.
  ![Fletching Works](docs/images/banner.png)
-->

## Why install it

- **The fletching table finally does something.** In vanilla it is a villager workstation and nothing else. Now it opens a crafting UI.
- **No more arrow shortages.** Out of flint? Cobblestone, copper and iron all work, so you craft from whatever you already have.
- **Fifteen arrows, fifteen jobs.** Demolition, remote redstone, self-teleport, lightning — each one is worth carrying on its own.

## Installation

1. Install **Fabric Loader ≥ 0.17.3** for **Minecraft 1.21.11** (Java 21).
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Drop `fletching-works-1.1.0.jar` into `mods/`.

That is all for singleplayer. **For a server, install it on both sides** — the mod registers blocks, items and a menu, so a client-only install cannot join.

## Download

| Source | Version | Notes |
| --- | --- | --- |
| [GitHub Releases](https://github.com/ztqjntm/Fletching-Works/releases) | 1.1.0 | Grab the jar under `Assets` |
| [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fletching-works) | 1.1.0 | One-click install via the CurseForge app |
| Modrinth | — | Not listed yet |

| Mod version | Minecraft | Loader |
| --- | --- | --- |
| 1.1.0 | 1.21.11 | Fabric |

## The fifteen arrows

| Arrow | Damage bonus | Effect |
| --- | --- | --- |
| Stone Arrow | +0 | — |
| Copper Arrow | +0 | Summons real lightning on a thunderstorm hit under open sky |
| Iron Arrow | +1 | — |
| Gold Arrow | −1 | — |
| Diamond Arrow | +2 | — |
| Netherite Arrow | +4 | — |
| TNT Arrow | −1 | Explodes on impact with a block or an entity |
| Redstone Arrow | −1 | Fires a redstone pulse where it lands |
| Pearl Arrow | ±0 | Teleports the shooter to the impact point |
| Web Arrow | −1 | Leaves an 8-second cobweb where it lands |
| Fire Arrow | −1 | Ignites the target and spreads a 3x3 fire |
| Bee Arrow | −1 | Summons 2 bees that only attack the target |
| Frost Arrow | −1 | Freezes the target for ~5 s (fully frozen ~1.5 s); freezes 3x3 water |
| Wind Charge Arrow | −1 | Pure knockback in a 3x3x3 area |
| Grapple Arrow | −1 | Pulls you to a block, or a creature 5 blocks toward you (1.5 s cooldown) |

The bonus is added **directly to vanilla's final damage** (vanilla rolls speed and crits, rounds up, then the bonus is applied, floored at 0). So a diamond arrow is not "double damage" — it is a flat +2.

<details>
<summary>Details on the ten special arrows</summary>

- **TNT Arrow** — radius 4, no fire, **destroys blocks** (TNT strapped to an arrow — it is consumed on impact and cannot be recovered). Entity damage is halved, so point-blank shots will not instantly kill you. With `mobGriefing` off it only damages entities and leaves the terrain alone.
- **Redstone Arrow** — on hitting a block, it triggers buttons, levers, doors, fence gates, trapdoors, bells, candles and bee nests within 2 blocks (without breaking anything), and places an invisible block on the face it hit that **emits signal strength 15 for 8 ticks** — that is what redstone lamps, pistons, dust, dispensers and rails actually respond to. The arrow itself sticks in the block as usual and can be picked up again.
  - It **swaps out whatever occupies that face and puts it back after 8 ticks**, so snow layers, tall grass, seagrass and flowing water all work — nothing gets eaten.
  - Hitting the same spot again just resets the timer; it does not stack a second block.
- **Pearl Arrow** — teleports the **shooter** to the impact point: 0.6 blocks back along the arrow's incoming direction, then a collision check within 4 blocks of height for a spot that is not inside a block. Costs 3 fall damage (a vanilla ender pearl costs 5). The arrow is consumed — **unless the teleport cannot happen** (e.g. a dispenser-fired arrow has no owner), in which case it sticks in the ground like any other arrow.
- **Copper Arrow** — only works during a **thunderstorm** with **open sky** above the impact point; underground or indoors it is an ordinary arrow. The bolt is real: it sets fires and can strike you and nearby mobs. Damage and kills are credited to the shooter.
- **Web Arrow** — leaves a cobweb that vanishes after 8 seconds. On an entity it goes under their feet; on a block it goes on the face you hit. If there is no replaceable spot, it only deals damage. The web traps the shooter too — point-blank shots are a bad idea.
- **Fire Arrow** — ignites the target for 5 seconds and spreads a 3x3 fire at the impact. It is normal fire and **it spreads**, so do not shoot your own house. Rain, water and `doFireTick=false` limit it as usual.
- **Bee Arrow** — on an entity, summons 2 bees that only attack that target and leave after 30 seconds. Bees die after stinging, so it is really 2 stings. Block hits and ownerless dispenser arrows spawn nothing.
- **Frost Arrow** — freezes the target for about 5 seconds (fully frozen for the first ~1.5 s, since vanilla decays 2 ticks per tick) and turns 3x3 water sources into frosted ice, which melts on its own. Only source water is frozen; any piece of leather armor makes a target immune. **The arrow is not consumed.**
- **Wind Charge Arrow** — pure knockback for every entity in a 3x3x3 area. No damage, no block destruction, no redstone triggers; it can push the shooter too.
- **Grapple Arrow** — pulls you to a block within 32 blocks, or pulls a creature up to 5 blocks toward you. It moves per tick and stops at walls. Each successful pull has a **1.5 second cooldown** (the arrow in your inventory shows it); hits during the cooldown do nothing. **The arrow is not consumed**; ownerless dispenser arrows do not pull.

</details>

## The fletching table

Right-click a fletching table to open the UI. The three input slots are **fixed**: tip top-right, stick in the middle, feather bottom-left; empty slots show a faint hint of what goes in, so you do not need JEI to get started. Wrong items simply will not go in. Once all three are filled the output slot shows your arrows, and taking them consumes one of each input.

| Tip material | Output | Per craft |
| --- | --- | --- |
| Flint | Arrow | 4 |
| Cobblestone | Stone Arrow | 4 |
| Copper Ingot | Copper Arrow | 4 |
| Iron Ingot | Iron Arrow | 4 |
| Gold Ingot | Gold Arrow | 4 |
| Diamond | Diamond Arrow | 4 |
| Netherite Ingot | Netherite Arrow | 16 |
| Block of TNT | TNT Arrow | 1 |
| Redstone | Redstone Arrow | 3 |
| Ender Pearl | Pearl Arrow | 2 |
| String | Web Arrow | 2 |
| Blaze Powder | Fire Arrow | 2 |
| Honeycomb | Bee Arrow | 1 |
| Blue Ice | Frost Arrow | 2 |
| Wind Charge | Wind Charge Arrow | 2 |
| Tripwire Hook | Grapple Arrow | 2 |

A few rules:

- The output is taken **as a whole batch** (right-click included), so you can never take 1 arrow and pay for a full set of materials.
- **The table never keeps your items.** Closing the UI hands the inputs back to your inventory (whatever does not fit drops at your feet), so nothing can be forgotten inside.
- **Each player gets their own three slots.** Two players opening the same table cannot see or take each other's materials; closing the UI returns only what that player put in.
- **Breaking the table** while someone has it open drops whatever is inside at its position. The output slot is a preview and never drops.
- **If the table is destroyed by an explosion or piston**, anything inside drops at its old position within about a second instead of being stranded in the save.

## Infinity II and Infinity III

Two independent enchantments for bows and crossbows, built on 1.21.11's `minecraft:ammo_use` component. Both keep normal arrows free; the difference only shows on the 15 special arrows (plus tipped and spectral arrows).

| Enchantment | Normal arrows | Special arrows | Where to get |
| --- | --- | --- | --- |
| Infinity II | never consumed | 20% chance the arrow is not consumed | Enchanting table (min 20), fishing treasure (2% per treasure roll), stronghold loot (3–10%) |
| Infinity III | never consumed | 33% chance the arrow is not consumed | Enchanting table (min 30), fishing treasure (0.5%), stronghold loot (1–4%) |

Both are mutually exclusive with each other, with vanilla Infinity and with Mending. Firework rockets are never refunded. The refund is resolved at the ammo step, and a refunded arrow is marked creative-only — it cannot be picked up, so there is no duplication.

## Compatibility & known limitations

- **Fabric and Minecraft 1.21.11 only.** No Forge / NeoForge plans.
- **Recipes are datapack-driven**: every recipe lives in `data/update-ft/recipe/*.json` as `update-ft:fletching`, so packs can add or change them and `/reload` applies it. The tip slot's whitelist is a separate tag (`update-ft:fletching_tips`) — add your material there too, or it will not fit into the slot.
- **JEI is supported out of the box.** JEI is only needed on the client: the mod registers its own recipe serializer with Fabric's sync API, so the recipe data reaches the client without JEI on the server. REI / EMI are not supported yet.
- **The redstone pulse only powers the cell adjacent to the face it hit.** To light a lamp further away, route the signal with dust.
- **TNT and fire arrows grief terrain.** On a multiplayer server that is infinite TNT and infinite fire; pair it with claim/protection plugins, or tune them down in `config/fletching-works.json` (`tntArrow.enabled`, `tntArrow.breaksBlocks`, `fireArrow.spread`). With `mobGriefing` off, TNT arrows only damage entities, and fire arrows no longer place any fire (they still ignite the creature they hit).
- **The fletching table has no block entity**: contents are held in the level's saved data keyed by block position and player, so each player only sees their own materials, closing the UI returns them, and a crash does not eat them. Hoppers still cannot feed the table and nothing is displayed on it.
- **Empty the tables before uninstalling the mod.** Contents live per dimension in `world/dimensions/minecraft/<dimension>/data/update_ft_fletching_tables.dat` (overworld, the nether and the end each have one file), which only this mod reads. Run `/fletchingworks dump` (permission level 2) first — it drops everything in the current dimension at your feet.
- **Infinity only works with vanilla arrows.** The enchantment predicate matches `minecraft:arrow`, not the `#minecraft:arrows` tag, so the 15 new arrows are always consumed.

## Configuration

Server-side settings live in `config/fletching-works.json`, generated on first launch. Edit it and run `/fletchingworks reload` (permission level 2) — no restart needed.

| Key | Default | What it does |
| --- | --- | --- |
| `tntArrow.enabled` | `true` | Turn the explosion off; the arrow then sticks like a normal arrow |
| `tntArrow.breaksBlocks` | `true` | Still requires `mobGriefing` |
| `tntArrow.entityDamageMultiplier` | `0.5` | `1.0` = vanilla TNT damage |
| `fireArrow.spread` | `1` | Fire radius: `0` = one block, `1` = 3×3, `2` = 5×5, `3` = 7×7 |
| `fireArrow.igniteSeconds` | `5` | Burn time applied to a creature |
| `lightningArrow.requiresThunderstorm` | `true` | `false` lets copper arrows call lightning in clear weather |
| `grappleArrow.cooldownTicks` | `30` | `0` disables the cooldown |
| `grappleArrow.maxDistance` | `32.0` | Maximum pull distance, in blocks |
| `table.returnItemsOnClose` | `true` | `false` keeps the materials in the table |

## FAQ

**Do I need Fabric API?**
Yes. Without it the game fails to start.

**Do I need it on the server?**
Yes. The mod registers blocks, items and a menu, so client and server must both have it.

**Can I pick my arrows back up?**
Normal, copper, redstone, frost and grapple arrows stick in the ground and can be retrieved. TNT and wind charge arrows are consumed on impact; pearl arrows are consumed once the teleport succeeds. Web, fire and bee arrows are only consumed when their effect actually happens (a web is placed, fire is lit, bees are summoned) — otherwise they stick in the ground like any other arrow.

**Will it conflict with other mods?**
It only rewrites vanilla arrow impact logic (one Mixin) and does not touch other mods' content. If something does break, open an issue with `latest.log` and reproduction steps.

**Any plans for 1.20.x / 1.21.1?**
1.21.11 only for now. Other versions are neither tested nor scheduled.

## Building from source

```bash
./gradlew build        # gradlew.bat on Windows
./gradlew test         # unit tests only
```

Output lands in `build/libs/`: `fletching-works-1.1.0.jar` (the one you install) and `-sources.jar`. Requires JDK 21.

Project layout, what each file does, the registration and crafting flow, and the release process all live in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) (Chinese).

## Feedback

- Bugs / ideas: [Issues](https://github.com/ztqjntm/Fletching-Works/issues)
- Changelog: [CHANGELOG.md](CHANGELOG.md)
- Roadmap: [TODO.md](TODO.md)
- Author: [Bilibili](https://space.bilibili.com/3546940667071250)

## License

[Mozilla Public License 2.0](LICENSE). Use, modify and redistribute freely; modified MPL files must stay open under the same license.

<details>
<summary>Technical details</summary>

| Field | Value |
| --- | --- |
| Mod ID | `update-ft` |
| Version | 1.1.0 |
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.17.3 |
| Fabric API | ≥ 0.139.4 |
| Java | 21 |
| Package | `ztqjntm.update` |
| Environment | Client + server (`*`) |
| License | MPL-2.0 |

</details>
