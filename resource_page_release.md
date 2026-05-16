# BlockDoom - Multiplayer Survival Chaos

![BlockDoom Resource Icon](file:///C:/Users/jeeln/.gemini/antigravity/brain/dd54adbc-7f3c-4184-9980-7dc7183ec973/blockdoom_resource_icon_1778920890972.png)

## ⚡ Tagline
> **"Survive the inevitable. Every 60 seconds, a random block disintegrates from your world forever. Can you defeat the Ender Dragon before the world vanishes beneath your feet?"**

---

## 📖 Compelling Description

**BlockDoom** is the ultimate multiplayer survival challenge plugin designed for Minecraft Paper 1.20.4. It introduces a relentless, high-stakes game mode where time is your greatest enemy. 

As a global countdown ticks in the ActionBar, tension mounts. Every 60 seconds, BlockDoom samples the environment around active players, randomly selecting a naturally generated block material. A dramatic title announces the target block, and 5 seconds later, **every single natural instance of that block across the loaded world disintegrates into smoke and dust.**

Once a block material is erased, it is logged into an irreversible registry and can **never** be selected again. If `STONE` or `DIRT` vanishes, caves collapse and floating islands remain. If trees disintegrate, wood becomes priceless. The chaos continues mercilessly until your players either defeat the Ender Dragon or survival becomes mathematically impossible!

### 🌟 Key Features
- 🛡️ **Player Build Protection**: Blocks placed by your players are tracked in high-speed chunk memory and protected. Custom bases and structures stay safe while the natural world crumbles around them!
- 🖥️ **Immersive In-Game GUI (`/blockdoom ui`)**: A beautiful chest menu allowing server operators to instantly adjust countdown timers, deletion speeds, scan radiuses, and manage the block exclusion blacklist with intuitive clicks.
- 🔄 **Auto-Reloading**: Tweak settings on the fly via UI or commands. Changes sync instantly across all active game loops without restarting your server.
- ⚡ **Zero Lag Architecture**: Engineered specifically for high-performance Paper servers. Uses asynchronous chunk snapshot scanning, batched main-thread deletion queues, and Persistent Data Container (PDC) chunk NBT tagging to keep server TPS at a flawless 20.0.
- 🌌 **Per-Dimension Isolation**: Overworld deletions are completely isolated from the Nether and End.

---

## 🎮 Complete Usage Guide: CLI & UI

BlockDoom is designed for effortless administration. You can control the entire game loop and configure all parameters using either the in-game GUI or command-line interface. All commands require the `blockdoom.admin` permission (given to server operators by default).

### 🖥️ 1. Using the Interactive GUI (`/blockdoom ui`)
Simply type `/blockdoom ui` in-game to open the main configuration chest menu.

```
+---------------------------------------------------+
|  [⏰]   [🔄]   [🧭]   [🔴]   [🛡️]   [🛒]   [🛑]   |
+---------------------------------------------------+
```

- ⏰ **Timer Duration (Clock)**: Left-Click to add `+10s`, Right-Click to subtract `-10s`. Shift-Left for `+60s`, Shift-Right for `-60s`. Instantly updates the countdown cycle length.
- 🔄 **Auto Reload Config (Repeater)**: Click to toggle (`True`/`False`). When enabled, any changes made in the GUI or CLI instantly synchronize across the server.
- 🧭 **Scan Radius (Compass)**: Left/Right Click to increase or decrease the chunk radius (`1` to `8` chunks) around players where Candidate blocks are sampled.
- 🔴 **Show Next Block Early (Torch/Lever)**: Click to toggle. When enabled, players see exactly which block will vanish in the real-time ActionBar timer (e.g. *"Next deletion: Stone in 00:43"*).
- 🛡️ **Protect Player Builds (Shield)**: Click to toggle (`True`/`False`). When enabled, manually built player structures are immune to deletion.
- 🛒 **Deletion Speed (Minecart)**: Left/Right Click to adjust the deletion throttle (`+5`/`-5` chunks per tick). Controls how fast blocks disintegrate across the world.
- 🛑 **Manage Blacklist (Barrier)**: Opens the multi-page **Blacklist Manager GUI**.

#### 🛑 Managing the Blacklist via GUI:
When you click the Barrier icon, the multi-page Blacklist Manager opens:
- **Removing Items**: The top half displays all currently blacklisted blocks (like Bedrock or Portals). Click any item to instantly remove it from the blacklist.
- **Adding Items**: Your personal inventory is shown in the bottom half. Click any block item in your personal inventory to instantly add it to the exclusion blacklist!

---

### 📜 2. Complete Command Reference

Every single command supports complete tab-completion for arguments, numbers, and material names.

| Command | Subcommand / Syntax | Description |
| :--- | :--- | :--- |
| `/blockdoom` | `/blockdoom` | Displays the help menu with all available commands. |
| `/blockdoom ui` | `/blockdoom ui` | Opens the interactive in-game configuration and blacklist menu. |
| `/blockdoom start` | `/blockdoom start` | Starts or resumes the global countdown and deletion loop. |
| `/blockdoom pause` | `/blockdoom pause` | Instantly pauses the active countdown or deletion cycle. |
| `/blockdoom skip` | `/blockdoom skip` | Skips the current countdown timer directly to the block reveal phase. |
| `/blockdoom status` | `/blockdoom status` | Displays the current game state, active dimension, target block, and total erased materials count. |
| `/blockdoom forcestart` | `/blockdoom forcestart` | Forcefully starts the game loop regardless of current state. |
| `/blockdoom forcedelete`| `/blockdoom forcedelete <material>` | Instantly forces the immediate deletion of a specific block material in the active dimension (e.g. `/blockdoom forcedelete GRASS_BLOCK`). |
| `/blockdoom config` | `/blockdoom config <setting> <value>` | On-the-fly CLI configuration update (see subcommands below). |
| `/blockdoom blacklist`| `/blockdoom blacklist <add\|remove> <material>` | Instantly adds or removes a specific block material from the exclusion blacklist (e.g. `/blockdoom blacklist add DIAMOND_ORE`). |
| `/blockdoom reload` | `/blockdoom reload` | Manually reloads `config.yml`, `deleted_materials.yml`, and placement registries from disk. |
| `/blockdoom regenerate` | `/blockdoom regenerate` | ⚠️ **Complete Reset**: Teleports players to safety, unloads and deletes custom gameplay worlds (`blockdoom_*`), generates pristine fresh worlds, and resets all deletion registries. |

#### ⚙️ CLI Config Subcommands (`/blockdoom config <setting> <val>`)
- `/blockdoom config timer <seconds>`: Sets the countdown cycle duration (e.g. `60`).
- `/blockdoom config radius <chunks>`: Sets player sampling chunk radius (e.g. `3`).
- `/blockdoom config speed <chunks>`: Sets how many chunks are scrubbed per tick (e.g. `10`).
- `/blockdoom config shownext <true|false>`: Toggles revealing the target block early in the timer.
- `/blockdoom config protect <true|false>`: Toggles whether player builds are protected from deletion.
- `/blockdoom config autoreload <true|false>`: Toggles instant configuration auto-reloading.

---

## 🚀 Installation & Setup
1. Download `BlockDoom-1.0.0.jar` and place it in your Paper `plugins/` directory.
2. Start your server (Paper 1.20.4, Java 21).
3. Connect in-game and use `/blockdoom ui` to customize your settings.
4. Run `/blockdoom start` and brace for impact!
