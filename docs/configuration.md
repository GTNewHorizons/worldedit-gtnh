# Configuration

For server owners. The settings are in `config/worldedit/worldedit.cfg`, and each option explains itself in a comment. After editing the file, apply it with [`/we reload`](commands/worldedit-reload.md). `noDoubleSlash` and the two lists in the `placement` category only apply after a restart.

## Coming from `worldedit.properties`

Older versions used `config/worldedit/worldedit.properties`. On the first start without a `worldedit.cfg`, the values are imported once and the old file is renamed to `worldedit.properties.migrated`. You can delete it after checking the new file.

To import again, delete `worldedit.cfg` and rename the old file back to `worldedit.properties`.

## Where each option went

| Old key | Category | New option |
|---|---|---|
| `profile` | `general` | `profile` |
| `no-double-slash` | `general` | `noDoubleSlash` |
| `log-commands` | `general` | `logCommands` |
| `log-file` | `general` | `logFile` |
| `use-in-creative` | `permissions` | `useInCreative` |
| `cheat-mode` | `permissions` | `cheatMode` |
| `default-max-changed-blocks` | `limits` | `defaultChangeLimit` |
| `max-changed-blocks` | `limits` | `maxChangeLimit` |
| `max-radius` | `limits` | `maxRadius` |
| `max-brush-radius` | `limits` | `maxBrushRadius` |
| `max-super-pickaxe-size` | `limits` | `maxSuperPickaxeSize` |
| `butcher-default-radius` | `limits` | `butcherDefaultRadius` |
| `butcher-max-radius` | `limits` | `butcherMaxRadius` |
| `default-max-polygon-points` | `limits` | `defaultMaxPolygonPoints` |
| `max-polygon-points` | `limits` | `maxPolygonPoints` |
| `default-max-polyhedron-points` | `limits` | `defaultMaxPolyhedronPoints` |
| `max-polyhedron-points` | `limits` | `maxPolyhedronPoints` |
| `history-size` | `limits` | `historySize` |
| `disallowed-blocks` | `limits` | `disallowedBlocks` |
| `wand-item` | `tools` | `wandItem` |
| `super-pickaxe-drop-items` | `tools` | `superPickaxeDropItems` |
| `super-pickaxe-many-drop-items` | `tools` | `superPickaxeManyDropItems` |
| `nav-wand-item` | `tools` | `navigationWandItem` |
| `nav-wand-distance` | `tools` | `navigationWandDistance` |
| `nav-use-glass` | `tools` | `navigationUseGlass` |
| `use-inventory` | `inventory` | `useInventory` |
| `use-inventory-override` | `inventory` | `useInventoryOverride` |
| `use-inventory-creative-override` | `inventory` | `useInventoryCreativeOverride` |
| `schematic-save-dir` | `files` | `schematicSaveDir` |
| `snapshots-dir` | `files` | `snapshotsDir` |
| `allow-symbolic-links` | `files` | `allowSymbolicLinks` |
| `place-last-blocks` | `placement` | `placeLastBlocks` |
| `place-normally-blocks` | `placement` | `placeNormallyBlocks` |

`register-help` and `allow-extra-data-values` did nothing, and are gone.

[All commands](README.md)
