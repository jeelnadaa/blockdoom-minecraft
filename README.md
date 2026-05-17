# BlockDoom - Multiplayer Survival Chaos Plugin

**BlockDoom** is a production-quality, heavily optimized survival chaos plugin for Minecraft Paper 1.20.4. Every cycle, a global countdown timer ticks down. When it expires, a random solid naturally generated block type existing across active dimensions is selected and revealed globally. After a 5-second warning, all naturally generated instances of that block type in the selected dimension are permanently disintegrated. The chaos continues until players defeat the Ender Dragon or survival becomes impossible!

---

## 🎮 Core Gameplay Mechanics & Advanced Features

- **Interactive In-Game GUI (`/blockdoom ui`)**: A fully immersive chest inventory menu allowing server admins to view, adjust, and toggle all settings (timer, speed, build protection, early reveal, auto-reload) with left/right/shift clicks and real-time chat/sound feedback. Includes a multi-page interactive **Blacklist Manager**.
- **Seamless Native Server Integration**: Runs directly inside your standard server worlds (`world`, `world_nether`, `world_the_end`) without creating any messy custom world directories or custom world generators.
- **Strict Whole Block Filtering**: Candidate blocks are strictly filtered to solid, naturally generated full blocks (e.g. Stone, Ores, Logs, Planks, Dirt, Basalt, Netherrack, End Stone). Small decorative plants, flowers, grass, kelp, vines, and fluids are perfectly excluded.
- **Probabilistic Dimension Selection**: Only dimensions containing active players are selected, weighted by player count (e.g. 3 players in Overworld and 1 in Nether = 75% Overworld, 25% Nether).
- **Dimension Isolation & Global Fallback**: Deletion cycles and registries are strictly isolated per dimension. Deleting Overworld Stone leaves Nether Stone perfectly safe. If the active dimension has 0 naturally generated blocks left, the plugin instantly searches remaining dimensions before ever triggering defeat.
- **Global Loaded Chunk Snapshot Scanning**: To ensure peak server performance, BlockDoom asynchronously inspects chunk snapshots of all loaded chunks across the dimension to build a comprehensive candidate block list without locking the main thread.
- **Player Placement Protection Toggle**: When enabled (`protect-player-builds`), any block manually placed by a player whose material is registered as deleted is tracked in highly efficient, persistent chunk-based storage. These blocks are **permanently safe** from deletion.
- **No-Repeat Guarantee**: Once a material is erased in a world, it is logged into `deleted_materials.yml` and can never be selected again.
- **TPS-Preserving Deletion Queue & PDC Caching**: Block deletions happen in batched full chunks per server tick. Chunks are permanently tagged in their NBT `PersistentDataContainer` (`scrub_cycle`), allowing the plugin to instantly bypass clean chunks on unloads/reloads for zero CPU overhead.

---

## ⚙️ Configuration Guide (`config.yml`)

The `config.yml` file allows full customization of gameplay timers, performance limits, blacklists, UI messages, and sound cues. When `auto-reload-on-config-change` is enabled, changes applied via `/blockdoom config` or `/blockdoom ui` take effect instantly across all game timers and subsystems.

```yaml
game:
  timer-duration: 60      # Duration of each countdown cycle in seconds before a block is selected.
  reveal-delay: 5         # Warning delay in seconds between revealing the block and starting deletion.
  show-next-block-during-timer: false # If true, reveals the next block in actionbar during the timer.
  protect-player-builds: true # If true, manually placed blocks are protected from deletion.
  auto-reload-on-config-change: true # If true, updates via command or GUI take effect instantly.
  enabled-dimensions:     # Standard gameplay world names used by the server.
    overworld: "world"
    nether: "world_nether"
    end: "world_the_end"

performance:
  chunks-per-tick: 10     # Number of full loaded chunks scrubbed for deletion per server tick (20 ticks/sec).
```

### Key Configuration Sections

| Section | Setting | Default | Description |
| :--- | :--- | :--- | :--- |
| `game` | `timer-duration` | `60` | The number of seconds in each cycle before a block is selected. |
| `game` | `reveal-delay` | `5` | The warning window (in seconds) during which players see the title announcement before deletion starts. |
| `game` | `show-next-block-during-timer` | `false` | When enabled (`true`), pre-selects the block at the *start* of the countdown and displays it in the ActionBar (e.g. "Next deletion: Stone in 00:43"). |
| `game` | `protect-player-builds` | `true` | When enabled (`true`), protects manually built structures. When disabled (`false`), player builds vanish along with natural blocks. |
| `game` | `auto-reload-on-config-change` | `true` | Instantly applies setting changes across all game timers and subsystems without needing `/blockdoom reload`. |
| `performance` | `chunks-per-tick` | `10` | How many full chunks in the deletion queue are scrubbed per server tick. |
| `blacklist` | List of Materials | Bedrock, Portals, Air, Fluids, etc. | Blocks listed here will **never** be selected for deletion. |
| `messages` | MiniMessage Strings | Custom | Rich text formatting for ActionBar countdowns, Titles, Subtitles, and chat broadcasts. |
| `sounds` | Bukkit Sound Enums | Custom | Sound effects played during timer ticks, reveals, disintegrations, victories, and defeats. |

---

## 📜 Command Reference

All commands require the permission `blockdoom.admin` (default: server operators). Full tab-completion is supported for all subcommands and material arguments.

| Command | Subcommand / Usage | Description |
| :--- | :--- | :--- |
| `/blockdoom` | `/blockdoom` | Shows the command help menu. |
| `/blockdoom ui` | `/blockdoom ui` | 🖥️ **Interactive Menu**: Opens an in-game GUI chest inventory to view/edit config values and manage the block blacklist. |
| `/blockdoom start` | `/blockdoom start` | Starts or resumes the global countdown and gameplay loop. |
| `/blockdoom pause` | `/blockdoom pause` | Pauses the active countdown or deletion cycle instantly. |
| `/blockdoom skip` | `/blockdoom skip` | Skips the current countdown timer directly to the block reveal phase. |
| `/blockdoom reset` | `/blockdoom reset` | **Game Reset**: Wipes all deletion registries and timers clean to restart the challenge. |
| `/blockdoom status` | `/blockdoom status` | Displays current game state, active dimension, target block, and total erased materials count. |
| `/blockdoom reload` | `/blockdoom reload` | Live reloads `config.yml`, `deleted_materials.yml`, and placement data instantly. |
| `/blockdoom forcestart` | `/blockdoom forcestart` | Forcefully starts the game loop. |
| `/blockdoom forcedelete`| `/blockdoom forcedelete <material>` | Instantly forces the deletion of a specific block material in the active dimension. |
| `/blockdoom config` | `/blockdoom config <timer\|shownext\|speed\|protect\|autoreload> <val>` | On-the-fly CLI config update for timer duration, early reveal, speed, build protection, or auto-reload. |
| `/blockdoom blacklist`| `/blockdoom blacklist <add\|remove> <material>` | CLI command to instantly add or remove a block from the exclusion blacklist. |

---

## 📁 Data Storage & Persistence

BlockDoom stores runtime data inside `plugins/BlockDoom/`:
- `config.yml`: The main configuration file.
- `deleted_materials.yml`: Registry of all materials that have been permanently deleted in the current world run.
- `player_placements.yml`: Packed integer coordinates of all player-placed protected blocks, mapped by world and chunk key.

---

## 🚀 Building & Installation

> [!IMPORTANT]
> **Please build the project first using the instructions below, and then copy the generated JAR file from `build/libs/` into your server's `plugins/` directory.**

### Prerequisites
- **Java 21** or higher installed and added to your system `PATH`.
- A Minecraft **Paper 1.20.4** server environment.

### 1. Building the Plugin
You can compile the project instantly using the included Gradle wrapper without needing to install Gradle globally:

#### On Windows (PowerShell / Command Prompt):
```cmd
cd d:\blockdoom
.\gradlew.bat build
```

#### On Linux / macOS (Terminal):
```bash
./gradlew build
```

### 2. Locating Your Compiled JAR File
Upon a successful build, your ready-to-run plugin JAR is located exactly here:
```
d:\blockdoom\build\libs\BlockDoom-1.0.0.jar
```

### 3. Installation Steps
1. Copy `BlockDoom-1.0.0.jar` from `build/libs/` into your Paper server's `plugins/` folder.
2. Start or restart your server.
3. The plugin will automatically generate its configuration files upon startup.
4. When ready, join the server and execute `/blockdoom start` (or `/blockdoom ui` to configure) to initiate the chaos!
