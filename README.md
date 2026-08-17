# ModScript — In-Game Mod Creator for Minecraft 1.21.1

Create mods entirely from inside Minecraft using a simple, English-like programming language.

## Features

- **In-Game Code Editor** — full IDE with syntax highlighting, autocomplete, search, and error panel
- **ModScript Language** — natural-language syntax for creating items, blocks, mobs, effects, recipes, and abilities
- **Live Execution** — save and run scripts instantly without restarting the game
- **AI Assistant** — generate code, fix errors, explain scripts, and access tutorials
- **Version Control** — undo/redo with 50-version history per project
- **Mod Export** — export your creations as standalone .jar mods
- **Multiplayer Ready** — permission system with server-side validation
- **Debugging & Profiling** — step-through execution and performance analysis

---

## Installation

### Requirements
- Minecraft Java Edition 1.21.1
- NeoForge 21.1.248 or later
- Java 21

### Steps
1. Install NeoForge for Minecraft 1.21.1
2. Download `ModScript-1.0.0.jar` from Releases
3. Place the jar in your `mods/` folder
4. Launch Minecraft with NeoForge

---

## Quick Start

### Opening the Editor
Press the **Grave Accent** key (`` ` ``) to open the ModScript code editor.

### Your First Script
```modscript
create item "Ruby Sword"
damage: 12
durability: 800
speed: 1.8
```

Click **Save** (or press Ctrl+S), then click **Run** (or press F5).

### Creating a Mob
```modscript
create mob "Fire Goblin"
health: 60
attack: 18
speed: 0.4
base: zombie
```

### Adding Events
```modscript
when player attacks zombie:
    set on fire for 5 seconds
    deal 20 damage
    play "entity.ghast.shoot"
```

---

## ModScript Language Reference

### Creating Items
```modscript
create item "Item Name"
damage: 10          # Attack damage in hearts
durability: 500     # Number of uses before breaking
speed: 1.6          # Attack speed multiplier
heal: 5             # Health restored when eaten
food: 8             # Hunger points restored
```

### Creating Blocks
```modscript
create block "Block Name"
hardness: 5         # How hard to break (1-100)
```

### Creating Mobs
```modscript
create mob "Mob Name"
health: 50          # Health in hearts
attack: 15          # Attack damage
speed: 0.3          # Movement speed
base: zombie        # Base mob type (zombie, skeleton, creeper, spider, enderman)
```

### Creating Effects
```modscript
create effect "Effect Name"
type: poison        # Effect type
duration: 30        # Duration in seconds
level: 2            # Effect level
```

### Creating Recipes
```modscript
create recipe "Recipe Name"
pattern: " I | I | S "    # 3x3 grid (use letters, | separates rows)
result: "Iron Sword"       # What item is crafted
```

### Creating Abilities
```modscript
create ability "Ability Name"
damage: 30
range: 10
```

---

## Events

Events trigger actions when something happens in the game.

| Event | Description |
|-------|-------------|
| `player attacks [mob]` | Player hits a mob |
| `player breaks [block]` | Player breaks a block |
| `player joins` | Player logs into the server |
| `player clicks [item]` | Player right-clicks with an item |
| `player places [block]` | Player places a block |
| `player hurts` | Player takes damage |
| `player dies` | Player dies |
| `player sneaks` | Player crouches |
| `player jumps` | Player jumps |
| `player swims` | Player swims in water |
| `player crafts [item]` | Player crafts an item |

### Event Example
```modscript
when player attacks zombie:
    set on fire for 3 seconds
    deal 15 damage
    spawn "Skeleton"
    play "entity.skeleton.ambient"
```

---

## Actions

Actions are things that happen when an event triggers.

| Action | Description | Example |
|--------|-------------|---------|
| `give [qty] "[item]"` | Give items to player | `give 1 "Ruby Sword"` |
| `teleport [x] [y] [z]` | Move player | `teleport 0 64 0` |
| `spawn "[mob]"` | Create a mob nearby | `spawn "Fire Goblin"` |
| `remove all` | Remove nearby entities | `remove all` |
| `apply "[effect]" for [sec]` | Add potion effect | `apply "speed" for 30 seconds` |
| `heal [amount]` | Restore health | `heal 10` |
| `shoot` | Fire an arrow | `shoot` |
| `play "[sound]"` | Play a sound | `play "entity.experience_orb.pickup"` |
| `deal [damage]` | Damage nearby entities | `deal 20` |
| `set on fire for [sec]` | Ignite player | `set on fire for 5 seconds` |

---

## Editor Controls

| Key | Action |
|-----|--------|
| `` ` `` (Grave Accent) | Open/close editor |
| Ctrl+S | Save script |
| F5 | Run script |
| Ctrl+F | Find/search |
| Ctrl+Space | Autocomplete |
| Tab | Indent |
| Enter | New line (auto-indent) |
| Home | Start of line |
| End | End of line |
| Delete | Delete forward |
| Backspace | Delete backward |

---

## Commands

### Project Management
```
/modcreator create <name>          Create a new project
/modcreator list                   List all projects
/modcreator open <name>            Open a project
/modcreator save <project> <script> Save script to project
/modcreator run <project> <script>  Run script
```

### Version Control
```
/modcreator undo <project>         Undo last change
/modcreator redo <project>         Redo last change
/modcreator versions <project>     View version history
```

### AI Assistant
```
/modcreator ai generate <desc>     Generate code from description
/modcreator ai fix <script>        Suggest fixes for errors
/modcreator ai explain <script>    Explain what script does
/modcreator ai tutorial            Show full tutorial
```

### Advanced Tools
```
/modcreator export <project> <script>  Export as .jar mod
/modcreator debug <project>            Start debug session
/modcreator profile <project>          Profile performance
/modcreator version                    Show MC/NeoForge version
```

---

## AI Code Generation

The AI assistant can generate code from natural descriptions:

```
/modcreator ai generate "sword"        → Creates a sword with default stats
/modcreator ai generate "boss mob"     → Creates a powerful boss mob
/modcreator ai generate "fire event"   → Creates a fire-damage event
/modcreator ai generate "heal on join" → Creates a heal-on-join event
/modcreator ai generate "armor"        → Creates chestplate armor
/modcreator ai generate "potion"       → Creates a healing potion
```

---

## Exporting Mods

Export your ModScript projects as standalone NeoForge mods:

```
/modcreator export MyProject "create item \"Ruby Sword\"\ndamage: 12"
```

This creates:
- `MyProject-1.0.0.jar` — Installable mod jar
- `build.gradle` — Gradle build file
- `src/` — Generated Java source code
- `neoforge.mods.toml` — Mod metadata

The exported jar can be shared and installed on any NeoForge 1.21.1 server.

---

## Multiplayer

### Permissions
Permissions are stored in `modscript/permissions.json` in the world folder.

| Permission | Description |
|------------|-------------|
| `modscript.view` | View projects (default) |
| `modscript.run` | Run scripts (default) |
| `modscript.create` | Create new mods |
| `modscript.edit` | Edit existing mods |
| `modscript.delete` | Delete mods |
| `modscript.admin` | Full access |
| `modscript.export` | Export mods as .jar |

Operators (op level 2+) automatically have all permissions.

### Server Validation
- Scripts are limited to 50,000 characters
- Maximum 100 definitions per script
- Names must be alphanumeric (no special characters)
- Restricted names: minecraft, forge, neoforge, modscript, admin, op

---

## Debugging

Start a debug session:
```
/modcreator debug MyProject
```

Features:
- Step-through execution
- Breakpoints (up to 20)
- Variable inspection
- Execution status tracking

---

## Profiling

Profile script performance:
```
/modcreator profile MyProject
```

Output includes:
- Total execution time
- Operation count
- Average time per operation
- Slowest operation
- Memory usage before/after

---

## Example Projects

### Simple Sword
```modscript
create item "Iron Sword"
damage: 8
durability: 250
speed: 1.6
```

### Fire Mage Build
```modscript
create item "Fire Staff"
damage: 15
durability: 1000

create effect "Burn"
type: poison
duration: 10

when player attacks zombie:
    apply "Burn" for 10 seconds
    set on fire for 5 seconds
    deal 25 damage
    play "entity.ghast.shoot"
```

### Monster Spawner
```modscript
create mob "Dark Knight"
health: 100
attack: 30
speed: 0.5
base: skeleton

when player breaks stone:
    spawn "Dark Knight"
    play "entity.enderman.teleport"
```

### Healing Station
```modscript
when player joins:
    heal 20
    apply "regeneration" for 60 seconds
    apply "speed" for 30 seconds
    play "entity.player.levelup"
```

---

## Troubleshooting

**Editor won't open:** Ensure you're running NeoForge 1.21.1 and Java 21.

**Script won't run:** Check the error panel in the editor for syntax errors.

**Items not appearing:** Run the script again with F5 after saving.

**Permission denied:** Ask a server operator to grant you `modscript.create` permission.

**Export fails:** Ensure your script has no errors and uses valid names.

---

## Version Information

- **ModScript:** 1.0.0
- **Minecraft:** 1.21.1
- **NeoForge:** 21.1.248
- **Java:** 21

---

## License

MIT License

## Links

- [GitHub Repository](https://github.com/THTStreamer/ModScript)
- [Issue Tracker](https://github.com/THTStreamer/ModScript/issues)
