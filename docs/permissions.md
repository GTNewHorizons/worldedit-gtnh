# Permissions

For server owners: who can use WorldEdit, and how to give finer access.

## Without ServerUtilities

Only operators can use WorldEdit. Two options in the `permissions` category of the [configuration](configuration.md) open it up:

| Option | Effect |
|---|---|
| `useInCreative` | Players in creative mode can use everything. |
| `cheatMode` | Everyone can use everything. |

The nodes below make no difference here.

## With ServerUtilities

When [ServerUtilities](https://github.com/GTNewHorizons/ServerUtilities) is installed with its ranks enabled, every action checks a permission node, so ranks can allow or deny each one. A node no rank mentions is for operators only, so nothing changes until you set up ranks. `useInCreative` and `cheatMode` still allow everything on top.

A node also covers every node below it: allowing `worldedit` allows everything, and `worldedit.brush` allows every brush. Watch out for the nodes that have stronger ones below them, and deny those separately if needed:

| Allowing | Also allows |
|---|---|
| `worldedit.history.undo`, `worldedit.history.redo` | `.other`: undoing or redoing other players' edits |
| `worldedit.limit` | `worldedit.limit.unrestricted`: no limits at all |
| `worldedit.butcher` | Every `//butcher` flag, including pets and NPCs |
| `worldedit.superpickaxe` | The area and recursive super pickaxes |
| `worldedit.fill` | `//fillr` |

When a command lists two nodes, either one is enough.

## Commands

| Node | Commands |
|---|---|
| `worldedit.analysis.count` | [`//count`](commands/count.md) |
| `worldedit.analysis.distr` | [`//distr`](commands/distr.md) |
| `worldedit.biome.info` | [`//biomeinfo`](commands/biomeinfo.md) |
| `worldedit.biome.list` | [`//biomelist`](commands/biomelist.md) |
| `worldedit.biome.set` | [`//generatebiome`](commands/generatebiome.md), [`//setbiome`](commands/setbiome.md) |
| `worldedit.brush.apply` | [`//brush apply`](commands/brush-apply.md) |
| `worldedit.brush.butcher` | [`//brush butcher`](commands/brush-butcher.md) |
| `worldedit.brush.clipboard` | [`//brush clipboard`](commands/brush-clipboard.md) |
| `worldedit.brush.cylinder` | [`//brush cylinder`](commands/brush-cylinder.md) |
| `worldedit.brush.deform` | [`//brush deform`](commands/brush-deform.md) |
| `worldedit.brush.ex` | [`//brush ex`](commands/brush-ex.md) |
| `worldedit.brush.forest` | [`//brush forest`](commands/brush-forest.md) |
| `worldedit.brush.gravity` | [`//brush gravity`](commands/brush-gravity.md) |
| `worldedit.brush.lower` | [`//brush lower`](commands/brush-lower.md) |
| `worldedit.brush.options.mask` | [`//brush mask`](commands/brush-mask.md) |
| `worldedit.brush.options.material` | [`//brush material`](commands/brush-material.md) |
| `worldedit.brush.options.range` | [`//brush range`](commands/brush-range.md) |
| `worldedit.brush.options.size` | [`//brush size`](commands/brush-size.md) |
| `worldedit.brush.paint` | [`//brush paint`](commands/brush-paint.md) |
| `worldedit.brush.raise` | [`//brush raise`](commands/brush-raise.md) |
| `worldedit.brush.set` | [`//brush set`](commands/brush-set.md) |
| `worldedit.brush.smooth` | [`//brush smooth`](commands/brush-smooth.md) |
| `worldedit.brush.sphere` | [`//brush sphere`](commands/brush-sphere.md) |
| `worldedit.butcher` | [`//butcher`](commands/butcher.md) |
| `worldedit.calc` | [`//calc`](commands/calc.md) |
| `worldedit.cancel` | [`//cancel`](commands/cancel.md) |
| `worldedit.chunkinfo` | [`//chunkinfo`](commands/chunkinfo.md) |
| `worldedit.clipboard.clear` | [`//clearclipboard`](commands/clearclipboard.md) |
| `worldedit.clipboard.copy` | [`//copy`](commands/copy.md) |
| `worldedit.clipboard.cut` | [`//cut`](commands/cut.md) |
| `worldedit.clipboard.flip` | [`//flip`](commands/flip.md) |
| `worldedit.clipboard.load` | [`//schematic load`](commands/schematic-load.md) |
| `worldedit.clipboard.paste` | [`//paste`](commands/paste.md) |
| `worldedit.clipboard.rotate` | [`//rotate`](commands/rotate.md) |
| `worldedit.clipboard.save` | [`//schematic save`](commands/schematic-save.md) |
| `worldedit.drain` | [`//drain`](commands/drain.md) |
| `worldedit.extinguish` | [`//ex`](commands/ex.md) |
| `worldedit.fast` | [`//fast`](commands/fast.md) |
| `worldedit.fill` | [`//fill`](commands/fill.md) |
| `worldedit.fill.recursive` | [`//fillr`](commands/fillr.md) |
| `worldedit.fixlava` | [`//fixlava`](commands/fixlava.md) |
| `worldedit.fixwater` | [`//fixwater`](commands/fixwater.md) |
| `worldedit.generation.cylinder` | [`//cyl`](commands/cyl.md), [`//hcyl`](commands/hcyl.md) |
| `worldedit.generation.forest` | [`//forestgen`](commands/forestgen.md) |
| `worldedit.generation.pumpkins` | [`//pumpkins`](commands/pumpkins.md) |
| `worldedit.generation.pyramid` | [`//hpyramid`](commands/hpyramid.md), [`//pyramid`](commands/pyramid.md) |
| `worldedit.generation.shape` | [`//generate`](commands/generate.md), [`//generatebiome`](commands/generatebiome.md) |
| `worldedit.generation.sphere` | [`//hsphere`](commands/hsphere.md), [`//sphere`](commands/sphere.md) |
| `worldedit.global-mask` | [`//gmask`](commands/gmask.md) |
| `worldedit.green` | [`//green`](commands/green.md) |
| `worldedit.help` | [`//help`](commands/help.md), [`/worldedit help`](commands/worldedit-help.md) |
| `worldedit.history.clear` | [`//clearhistory`](commands/clearhistory.md) |
| `worldedit.history.redo` | [`//redo`](commands/redo.md) |
| `worldedit.history.undo` | [`//undo`](commands/undo.md) |
| `worldedit.light.fix` | [`//fixlighting`](commands/fixlighting.md), [`//getlighting`](commands/getlighting.md) |
| `worldedit.light.remove` | [`//removelight`](commands/removelight.md) |
| `worldedit.light.set` | [`//setblocklight`](commands/setblocklight.md), [`//setskylight`](commands/setskylight.md) |
| `worldedit.limit` | [`//limit`](commands/limit.md) |
| `worldedit.listchunks` | [`//listchunks`](commands/listchunks.md) |
| `worldedit.navigation.ascend` | [`//ascend`](commands/ascend.md) |
| `worldedit.navigation.ceiling` | [`//ceil`](commands/ceil.md) |
| `worldedit.navigation.descend` | [`//descend`](commands/descend.md) |
| `worldedit.navigation.jumpto.command` | [`//jumpto`](commands/jumpto.md) |
| `worldedit.navigation.thru.command` | [`//thru`](commands/thru.md) |
| `worldedit.navigation.unstuck` | [`//unstuck`](commands/unstuck.md) |
| `worldedit.navigation.up` | [`//up`](commands/up.md) |
| `worldedit.regen` | [`//regen`](commands/regen.md) |
| `worldedit.region.center` | [`//center`](commands/center.md) |
| `worldedit.region.curve` | [`//curve`](commands/curve.md) |
| `worldedit.region.deform` | [`//deform`](commands/deform.md) |
| `worldedit.region.faces` | [`//faces`](commands/faces.md) |
| `worldedit.region.flora` | [`//flora`](commands/flora.md) |
| `worldedit.region.forest` | [`//forest`](commands/forest.md) |
| `worldedit.region.hollow` | [`//hollow`](commands/hollow.md) |
| `worldedit.region.line` | [`//line`](commands/line.md) |
| `worldedit.region.move` | [`//move`](commands/move.md) |
| `worldedit.region.naturalize` | [`//naturalize`](commands/naturalize.md) |
| `worldedit.region.overlay` | [`//overlay`](commands/overlay.md) |
| `worldedit.region.replace` | [`//replace`](commands/replace.md) |
| `worldedit.region.set` | [`//set`](commands/set.md) |
| `worldedit.region.smooth` | [`//smooth`](commands/smooth.md) |
| `worldedit.region.stack` | [`//stack`](commands/stack.md) |
| `worldedit.region.walls` | [`//walls`](commands/walls.md) |
| `worldedit.reload` | [`/worldedit reload`](commands/worldedit-reload.md), [`/worldedit reloadmappings`](commands/worldedit-reloadmappings.md) |
| `worldedit.remove` | [`//remove`](commands/remove.md) |
| `worldedit.removeabove` | [`//removeabove`](commands/removeabove.md) |
| `worldedit.removebelow` | [`//removebelow`](commands/removebelow.md) |
| `worldedit.removenear` | [`//removenear`](commands/removenear.md) |
| `worldedit.replacenear` | [`//replacenear`](commands/replacenear.md) |
| `worldedit.schematic.delete` | [`//schematic delete`](commands/schematic-delete.md) |
| `worldedit.schematic.formats` | [`//schematic formats`](commands/schematic-formats.md) |
| `worldedit.schematic.list` | [`//schematic list`](commands/schematic-list.md) |
| `worldedit.schematic.load` | [`//schematic load`](commands/schematic-load.md) |
| `worldedit.schematic.save` | [`//schematic save`](commands/schematic-save.md) |
| `worldedit.selection.chunk` | [`//chunk`](commands/chunk.md) |
| `worldedit.selection.contract` | [`//contract`](commands/contract.md) |
| `worldedit.selection.expand` | [`//expand`](commands/expand.md) |
| `worldedit.selection.hpos` | [`//hpos1`](commands/hpos1.md), [`//hpos2`](commands/hpos2.md) |
| `worldedit.selection.inset` | [`//inset`](commands/inset.md) |
| `worldedit.selection.outset` | [`//outset`](commands/outset.md) |
| `worldedit.selection.pos` | [`//pos1`](commands/pos1.md), [`//pos2`](commands/pos2.md) |
| `worldedit.selection.shift` | [`//shift`](commands/shift.md) |
| `worldedit.selection.size` | [`//size`](commands/size.md) |
| `worldedit.snapshots.list` | [`//snapshot list`](commands/snapshot-list.md) |
| `worldedit.snapshots.restore` | [`//restore`](commands/restore.md), [`//snapshot after`](commands/snapshot-after.md), [`//snapshot before`](commands/snapshot-before.md), [`//snapshot sel`](commands/snapshot-sel.md), [`//snapshot use`](commands/snapshot-use.md) |
| `worldedit.snow` | [`//snow`](commands/snow.md) |
| `worldedit.superpickaxe` | [`//superpickaxe single`](commands/superpickaxe-single.md), [`//`](commands/superpickaxe.md) |
| `worldedit.superpickaxe.area` | [`//superpickaxe area`](commands/superpickaxe-area.md) |
| `worldedit.superpickaxe.recursive` | [`//superpickaxe recursive`](commands/superpickaxe-recursive.md) |
| `worldedit.thaw` | [`//thaw`](commands/thaw.md) |
| `worldedit.tool.data-cycler` | [`//tool cycler`](commands/tool-cycler.md) |
| `worldedit.tool.deltree` | [`//tool deltree`](commands/tool-deltree.md) |
| `worldedit.tool.farwand` | [`//tool farwand`](commands/tool-farwand.md) |
| `worldedit.tool.flood-fill` | [`//tool floodfill`](commands/tool-floodfill.md) |
| `worldedit.tool.info` | [`//tool info`](commands/tool-info.md) |
| `worldedit.tool.lrbuild` | [`//tool lrbuild`](commands/tool-lrbuild.md) |
| `worldedit.tool.replacer` | [`//tool repl`](commands/tool-repl.md) |
| `worldedit.tool.tree` | [`//tool tree`](commands/tool-tree.md) |
| `worldedit.wand` | [`//wand`](commands/wand.md) |

These need no permission: [`//brush`](commands/brush.md), [`//tool none`](commands/tool-none.md), [`//schematic`](commands/schematic.md), [`//searchitem`](commands/searchitem.md), [`//sel`](commands/sel.md), [`//snapshot`](commands/snapshot.md), [`//superpickaxe`](commands/superpickaxe.md), [`//toggleplace`](commands/toggleplace.md), [`//tool`](commands/tool.md), [`/worldedit cui`](commands/worldedit-cui.md), [`/worldedit tz`](commands/worldedit-tz.md), [`/worldedit version`](commands/worldedit-version.md), [`/worldedit`](commands/worldedit.md).

## Other nodes

| Node | What it allows |
|---|---|
| `worldedit.anyblock` | Use the blocks the configuration disallows. |
| `worldedit.butcher.pets`, `.npcs`, `.golems`, `.animals`, `.ambient`, `.tagged`, `.armorstands` | The matching flags of [`//butcher`](commands/butcher.md). `.lightning` is checked for `-l`, which does nothing at the moment. |
| `worldedit.history.undo.other`, `worldedit.history.redo.other` | Undo or redo another player's edits, with [`//undo`](commands/undo.md) and [`//redo`](commands/redo.md). |
| `worldedit.limit.unrestricted` | Ignore the configured limits on changed blocks and on selection points, and turn off your own limit with [`//limit`](commands/limit.md). |
| `worldedit.navigation.jumpto.tool` | Jump to where you look by left clicking with the navigation wand (the compass by default). |
| `worldedit.navigation.thru.tool` | Pass through walls by right clicking with the navigation wand. |
| `worldedit.override.bedrock` | Break bedrock with the super pickaxes, and start a flood fill from it. |

[All commands](README.md)
