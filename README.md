# BlockDoom - Multiplayer Survival Chaos Plugin

**BlockDoom** is a production-quality, heavily optimized survival chaos plugin for Minecraft Paper 1.20.4. Every cycle, a global countdown timer ticks down. When it expires, a random valid block type existing naturally near active players is selected and revealed globally. After a 5-second warning, all naturally generated instances of that block type in the selected dimension are permanently disintegrated. The chaos continues until players defeat the Ender Dragon or survival becomes impossible!

---

## 🎮 Core Gameplay Mechanics

- **Probabilistic Dimension Selection**: Only dimensions containing active players are selected, weighted by player count (e.g. 3 players in Overworld and 1 in Nether = 75% Overworld, 25% Nether).
- **Loaded Chunk Snapshot Scanning**: To ensure peak server performance, BlockDoom never scans the entire world. It asynchronously inspects chunk snapshots within a configurable radius around players.
- **Player Placement Protection**: Any block manually placed by a player is tracked in highly efficient, persistent chunk-based storage. Player-placed blocks are **permanently safe** from deletion.
- **No-Repeat Guarantee**: Once a material is erased, it is logged into `deleted_materials.yml` and can never be selected again.
- **TPS-Preserving Deletion Queue**: Block deletions happen in batched chunks per server tick with maximum block update caps, ensuring smooth server performance during massive ore/rock purges.

---

## ⚙️ Configuration Guide (`config.yml`)

The `config.yml` file allows full customization of gameplay timers, performance limits, blacklists, UI messages, and sound cues. Changes can be applied instantly without restarting the server by running `/blockdoom reload`.

```yaml
game:
  timer-duration: 60      # Duration of each countdown cycle in seconds before a block is selected.
  reveal-delay: 5         # Warning delay in seconds between revealing the block and starting deletion.
  scan-radius: 3          # Radius in chunks around active players to sample blocks (default = 3).
  enabled-dimensions:     # Dedicated gameplay world names used by the plugin.
    overworld: "blockdoom_overworld"
    nether: "blockdoom_nether"
    end: "blockdoom_end"

performance:
  chunks-per-tick: 10     # Number of loaded chunks processed for deletion per server tick (20 ticks/sec).
  max-blocks-per-chunk-tick: 500 # Max blocks deleted per chunk in a single tick to prevent physics lag.
```

### Key Configuration Sections

| Section | Setting | Default | Description |
| :--- | :--- | :--- | :--- |
| `game` | `timer-duration` | `60` | The number of seconds in each cycle before a block is selected. |
| `game` | `reveal-delay` | `5` | The warning window (in seconds) during which players see the title announcement before deletion starts. |
| `game` | `scan-radius` | `3` | The chunk radius around each active player from which candidate blocks are sampled. |
| `performance` | `chunks-per-tick` | `10` | How many chunks in the deletion queue are scrubbed per server tick. |
| `performance` | `max-blocks-per-chunk-tick`| `500` | Limits how many blocks vanish in a single chunk per tick. Prevents lighting and block update lag spikes. |
| `blacklist` | List of Materials | Bedrock, Portals, Air, Fluids, etc. | Blocks listed here will **never** be selected for deletion. |
| `messages` | MiniMessage Strings | Custom | Rich text formatting for ActionBar countdowns, Titles, Subtitles, and chat broadcasts. |
| `sounds` | Bukkit Sound Enums | Custom | Sound effects played during timer ticks, reveals, disintegrations, victories, and defeats. |

---

## 📜 Command Reference

All commands require the permission `blockdoom.admin` (default: server operators). Full tab-completion is supported for all subcommands and material arguments.

| Command | Subcommand / Usage | Description |
| :--- | :--- | :--- |
| `/blockdoom` | `/blockdoom` | Shows the command help menu. |
| `/blockdoom start` | `/blockdoom start` | Starts or resumes the global countdown and gameplay loop. |
| `/blockdoom pause` | `/blockdoom pause` | Pauses the active countdown or deletion cycle instantly. |
| `/blockdoom skip` | `/blockdoom skip` | Skips the current countdown timer directly to the block reveal phase. |
| `/blockdoom regenerate` | `/blockdoom regenerate` | **World Reset**: Safely teleports players to the root server world, wipes custom gameplay worlds (`blockdoom_*`), and generates fresh pristine worlds. All registries are reset. |
| `/blockdoom status` | `/blockdoom status` | Displays current game state, active dimension, target block, and total erased materials count. |
| `/blockdoom reload` | `/blockdoom reload` | Live reloads `config.yml`, `deleted_materials.yml`, and placement data instantly. |
| `/blockdoom forcestart` | `/blockdoom forcestart` | Forcefully starts the game loop. |
| `/blockdoom forcedelete`| `/blockdoom forcedelete <material>` | Instantly forces the deletion of a specific block material in the active dimension. |
| `/blockdoom config` | `/blockdoom config <timer\|radius> <val>` | On-the-fly CLI configuration update for timer duration or scan radius. |

---

## 📁 Data Storage & Persistence

BlockDoom stores runtime data inside `plugins/BlockDoom/`:
- `config.yml`: The main configuration file.
- `deleted_materials.yml`: Registry of all materials that have been permanently deleted in the current world run.
- `player_placements.yml`: Packed integer coordinates of all player-placed protected blocks, mapped by world and chunk key.

---

## 🚀 Building & Installation

### Prerequisites
- **Java 21** or higher installed and added to your system `PATH`.
- A Minecraft **Paper 1.20.4** server environment.

### Building the Plugin
You can compile the project using the included Gradle wrapper without needing to install Gradle globally:

#### On Windows (PowerShell / Command Prompt):
```cmd
.\gradlew.bat build
```

#### On Linux / macOS (Terminal):
```bash
./gradlew build
```

### Locating the Generated JAR File
Upon a successful build, the compiled plugin artifact is automatically generated in the `build/libs` directory:
```
d:/blockdoom/build/libs/BlockDoom-1.0.0.jar
```

### Installation Steps
1. Copy `BlockDoom-1.0.0.jar` from `build/libs/` into your Paper server's `plugins/` directory.
2. Start or restart your server.
3. The plugin will automatically generate its configuration files and custom gameplay worlds upon startup.
4. When ready, join the server and execute `/blockdoom start` to initiate the chaos!
