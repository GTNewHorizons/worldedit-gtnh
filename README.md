<img src="https://raw.githubusercontent.com/EngineHub/WorldEdit/master/worldedit-logo.svg" alt="WorldEdit" width="400">

[WorldEdit](https://github.com/EngineHub/WorldEdit/tree/forge-archive/1.7.10) for Minecraft 1.7.10, maintained for
[GT New Horizons](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack). Select an area, then fill it, copy it,
reshape it, regenerate it or restore it from a backup.

Requires [GTNHLib](https://github.com/GTNewHorizons/GTNHLib).

## What this fork adds

**Big edits don't freeze the server.** Edits are spread over server ticks within a time budget, so the server keeps
running while a large `//set`, `//copy`, `//paste`, `//undo` or `//regen` goes through. [`//cancel`](docs/commands/cancel.md)
stops your queued edits, and whatever was already done can be undone. Schematics load on a background thread.

**Restore from ServerUtilities backups.** [`/restore`](docs/commands/restore.md) reads the backups ServerUtilities
already makes, with no extra setup: select an area, pick a backup with [`/snapshot`](docs/commands/snapshot.md), and
put that area back the way it was.

**`//regen` works with modded world generation.** It regenerates chunks with the world's own generator, RWG included,
decorates them, and leaves the blocks outside your selection untouched, chests included.

**Made for modded blocks.**
- **Tile entities:** GregTech machines, chests and other blocks with stored data are copied, pasted and undone with
  their contents, and replacing them doesn't spill their inventories on the ground.
- **EndlessIDs:** block IDs and metadata above the vanilla limits work, as in `//set 3034:824`.
- **Attached blocks:** torches, doors, ProjectRed wires and gates, AE2 cables with covers and other ForgeMultipart
  blocks are placed after the blocks they sit on and removed before them, so nothing pops off or gets duplicated.
- **Fluids, plants and terrain:** modded fluids are drained by `//drain`, brushes aim through modded plants, and
  `//smooth -n` treats modded stone and dirt as terrain.
- **Rotation:** ArchitectureCraft, Carpenter's Blocks and ForgeMultipart microblocks turn correctly with `//rotate`.

**Safe on a server.**
- **Memory:** undo history is stored compactly and capped per player, so large edits don't use up the server's memory.
- **Permissions:** permission nodes are checked through ServerUtilities ranks when it is installed. See
  [permissions](docs/permissions.md).
- **Wand:** only the wand given by [`//wand`](docs/commands/wand.md) selects, so ordinary wooden axes work as tools.

**Modern commands.** Every command has tab completion and its own documentation page, and `//pos1`, `//pos2` and
`//chunk` accept `~` relative coordinates.

## Commands

See the [command documentation](docs/README.md) for everything you can type.

## Configuration

Settings are in `config/worldedit/worldedit.cfg`, see [configuration](docs/configuration.md). An old
`worldedit.properties` is imported once on first start. Who can use what is described in
[permissions](docs/permissions.md).

## Adding or changing a command

Commands are [Brigadier](https://github.com/Mojang/brigadier) trees, registered through GTNHLib. Each group in
`com.sk89q.worldedit.command` declares its whole tree in a `register` method, with the code of the commands below it.
The helpers are in `com.sk89q.worldedit.command.brigadier`.

Every command needs a page in `docs/commands`. `CommandDocsTest` fails if a command has no page, or if its page does not
show its real usage.
