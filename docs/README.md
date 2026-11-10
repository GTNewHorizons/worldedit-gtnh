# WorldEdit commands

WorldEdit lets you edit the world in bulk: select an area, then fill it, copy it, reshape it and more. This is the list of everything you can type.

## The basics

1. Get the wand with [`//wand`](commands/wand.md). Left click one corner, right click the opposite corner.
2. Run a command on your selection, like [`//set stone`](commands/set.md).
3. Changed your mind? [`//undo`](commands/undo.md).

Big edits run over several ticks, so the server keeps going. You get a message when they are done, and [`//cancel`](commands/cancel.md) stops them.

## Reading the commands

| Written as | Meaning |
|---|---|
| `<radius>` | You must type this |
| `[<radius>]` | You can leave this out |
| `[-h]` | A flag: a letter that switches something on |
| `[-m <mask>]` | A flag followed by a value |

- **Slashes.** Many commands start with two slashes, like `//set`. That is part of their name. Commands that start with one slash can also be typed with two.
- **Flags come first.** Put flags right after the command, before the other arguments: `//sphere -h stone 5`. Letters can be combined, so `-ao` is the same as `-a -o`.
- **Values with spaces** go in quotes: `//gmask "stone >dirt"`. A few arguments, such as formulas, take the rest of the line and need no quotes.
- **Numbers can be math.** Anywhere a number is asked for you can type a formula, like `//stack 2*8`. See [expressions](expressions.md). Whole numbers cut off decimals, so `5/2` is 2. A few arguments that mix numbers with other things take plain numbers only: the radius of `//sphere` and `//cyl`, coordinates such as in `//pos1`, the reverse amount of `//expand` and `//contract`, and the page after `-p`.
- **Tab completes** command names, flags and fixed choices.
- **Typing a command without its arguments** shows how to use it. [`//help`](commands/help.md) also lists the commands in the game.

## What you can type as an argument

- [Blocks and patterns](patterns.md): what to build with
- [Masks](masks.md): which blocks to touch
- [Directions](directions.md): which way to go
- [Tree types](trees.md): what to plant
- [Expressions](expressions.md): math formulas

Server owners can change the limits and defaults, see [configuration](configuration.md), and choose who can use what, see [permissions](permissions.md).

## All commands

### Selection

| Command | What it does |
|---|---|
| [`//pos1`](commands/pos1.md) | Set the first corner of your selection. |
| [`//pos2`](commands/pos2.md) | Set the second corner of your selection. |
| [`//hpos1`](commands/hpos1.md) | Set position 1 to the block you are looking at. |
| [`//hpos2`](commands/hpos2.md) | Set position 2 to the block you are looking at. |
| [`//chunk`](commands/chunk.md) | Select a whole chunk. |
| [`//wand`](commands/wand.md) | Get the selection wand. |
| [`//expand`](commands/expand.md) | Make your selection bigger. |
| [`//contract`](commands/contract.md) | Make your selection smaller. |
| [`//shift`](commands/shift.md) | Move your selection without touching any blocks. |
| [`//outset`](commands/outset.md) | Grow your selection in every direction. |
| [`//inset`](commands/inset.md) | Shrink your selection from every direction. |
| [`//size`](commands/size.md) | Show information about your selection. |
| [`//count`](commands/count.md) | Count a block in your selection. |
| [`//distr`](commands/distr.md) | List which blocks your selection is made of. |
| [`//sel`](commands/sel.md) | Choose how you select, or clear your selection. |

### Region

| Command | What it does |
|---|---|
| [`//set`](commands/set.md) | Fill your selection. |
| [`//line`](commands/line.md) | Draw a line between the two corners of your selection. |
| [`//curve`](commands/curve.md) | Draw a smooth curve through your selected points. |
| [`//replace`](commands/replace.md) | Replace blocks in your selection. |
| [`//overlay`](commands/overlay.md) | Put a layer on top of the blocks in your selection. |
| [`//center`](commands/center.md) | Set the blocks at the center of your selection. |
| [`//naturalize`](commands/naturalize.md) | Make terrain look natural. |
| [`//walls`](commands/walls.md) | Build the four sides of your selection. |
| [`//faces`](commands/faces.md) | Build all six sides of your selection. |
| [`//smooth`](commands/smooth.md) | Smooth the terrain in your selection. |
| [`//move`](commands/move.md) | Move the blocks in your selection. |
| [`//stack`](commands/stack.md) | Repeat your selection. |
| [`//regen`](commands/regen.md) | Regenerate your selection as the world generator made it. |
| [`//deform`](commands/deform.md) | Reshape your selection with a formula. |
| [`//hollow`](commands/hollow.md) | Hollow out what is in your selection. |
| [`//forest`](commands/forest.md) | Grow a forest in your selection. |
| [`//flora`](commands/flora.md) | Grow grass and flowers in your selection. |
| [`//fixlighting`](commands/fixlighting.md) | Recalculate the light in your selection. |
| [`//getlighting`](commands/getlighting.md) | Show the light at your position. |
| [`//removelight`](commands/removelight.md) | Remove the light in your selection. |
| [`//setblocklight`](commands/setblocklight.md) | Set the block light in your selection. |
| [`//setskylight`](commands/setskylight.md) | Set the sky light in your selection. |

### Generation

| Command | What it does |
|---|---|
| [`//sphere`](commands/sphere.md) | Generate a filled sphere or ellipsoid. |
| [`//hsphere`](commands/hsphere.md) | Generate a hollow sphere or ellipsoid. |
| [`//cyl`](commands/cyl.md) | Generate a filled cylinder. |
| [`//hcyl`](commands/hcyl.md) | Generate a hollow cylinder. |
| [`//pyramid`](commands/pyramid.md) | Generate a filled pyramid. |
| [`//hpyramid`](commands/hpyramid.md) | Generate a hollow pyramid. |
| [`//generate`](commands/generate.md) | Fill your selection according to a formula. |
| [`//generatebiome`](commands/generatebiome.md) | Set the biome of your selection according to a formula. |
| [`/forestgen`](commands/forestgen.md) | Generate a forest around you. |
| [`/pumpkins`](commands/pumpkins.md) | Generate pumpkin patches around you. |

### Clipboard

| Command | What it does |
|---|---|
| [`//copy`](commands/copy.md) | Copy your selection to your clipboard. |
| [`//cut`](commands/cut.md) | Cut your selection to your clipboard. |
| [`//paste`](commands/paste.md) | Paste your clipboard. |
| [`//rotate`](commands/rotate.md) | Rotate your clipboard. |
| [`//flip`](commands/flip.md) | Mirror your clipboard. |
| [`/clearclipboard`](commands/clearclipboard.md) | Empty your clipboard. |

### Schematics

| Command | What it does |
|---|---|
| [`/schematic`](commands/schematic.md) | Save and load schematic files. |
| [`/schematic load`](commands/schematic-load.md) | Load a schematic into your clipboard. |
| [`/schematic save`](commands/schematic-save.md) | Save your clipboard to a schematic. |
| [`/schematic delete`](commands/schematic-delete.md) | Delete a saved schematic. |
| [`/schematic formats`](commands/schematic-formats.md) | List the schematic formats. |
| [`/schematic list`](commands/schematic-list.md) | List the saved schematics. |

### History

| Command | What it does |
|---|---|
| [`//undo`](commands/undo.md) | Undo your last edit. |
| [`//redo`](commands/redo.md) | Redo what you undid. |
| [`//clearhistory`](commands/clearhistory.md) | Forget your edit history. |

### General

| Command | What it does |
|---|---|
| [`//limit`](commands/limit.md) | Change how many blocks one edit may change. |
| [`//cancel`](commands/cancel.md) | Stop your running and queued edits. |
| [`//fast`](commands/fast.md) | Turn fast mode on or off. |
| [`//gmask`](commands/gmask.md) | Limit all your edits to a mask. |
| [`//toggleplace`](commands/toggleplace.md) | Switch where things are placed. |
| [`//searchitem`](commands/searchitem.md) | Search for a block or item by name. |
| [`//help`](commands/help.md) | Show the commands and how to use them. |

### Utility

| Command | What it does |
|---|---|
| [`//fill`](commands/fill.md) | Fill a hole. |
| [`//fillr`](commands/fillr.md) | Fill a hole completely. |
| [`//drain`](commands/drain.md) | Remove water and lava around you. |
| [`//fixlava`](commands/fixlava.md) | Make the lava around you still. |
| [`//fixwater`](commands/fixwater.md) | Make the water around you still. |
| [`//removeabove`](commands/removeabove.md) | Remove the blocks above you. |
| [`//removebelow`](commands/removebelow.md) | Remove the blocks below you. |
| [`//removenear`](commands/removenear.md) | Remove a type of block around you. |
| [`//replacenear`](commands/replacenear.md) | Replace blocks around you. |
| [`//snow`](commands/snow.md) | Cover the area around you in snow. |
| [`//thaw`](commands/thaw.md) | Remove the snow around you. |
| [`//green`](commands/green.md) | Turn the dirt around you into grass. |
| [`//ex`](commands/ex.md) | Put out the fire around you. |
| [`/butcher`](commands/butcher.md) | Kill mobs around you. |
| [`/remove`](commands/remove.md) | Remove entities of a type around you. |
| [`//calc`](commands/calc.md) | Calculate a formula. |

### Tools

| Command | What it does |
|---|---|
| [`/tool`](commands/tool.md) | Bind a function to the item in your hand. |
| [`/none`](commands/none.md) | Unbind the tool from the item in your hand. |
| [`/info`](commands/info.md) | Bind the block information tool. |
| [`/tree`](commands/tree.md) | Bind the tree tool. |
| [`/repl`](commands/repl.md) | Bind the block replacer tool. |
| [`/cycler`](commands/cycler.md) | Bind the data value cycler tool. |
| [`/floodfill`](commands/floodfill.md) | Bind the flood fill tool. |
| [`/deltree`](commands/deltree.md) | Bind the floating tree remover. |
| [`/farwand`](commands/farwand.md) | Bind a selection wand that works at a distance. |
| [`/lrbuild`](commands/lrbuild.md) | Bind the long range building tool. |

### Super pickaxe

| Command | What it does |
|---|---|
| [`/superpickaxe`](commands/superpickaxe.md) | Choose how the super pickaxe works. |
| [`//`](commands/superpickaxe-toggle.md) | Turn the super pickaxe on or off. |
| [`/superpickaxe single`](commands/superpickaxe-single.md) | Break one block at a time. |
| [`/superpickaxe area`](commands/superpickaxe-area.md) | Break a cube of blocks. |
| [`/superpickaxe recursive`](commands/superpickaxe-recursive.md) | Break all connected blocks of the same type. |

### Brushes

| Command | What it does |
|---|---|
| [`/brush`](commands/brush.md) | Bind a brush to the item in your hand. |
| [`/brush sphere`](commands/brush-sphere.md) | Paint spheres. |
| [`/brush cylinder`](commands/brush-cylinder.md) | Paint cylinders. |
| [`/brush clipboard`](commands/brush-clipboard.md) | Paste your clipboard where you click. |
| [`/brush smooth`](commands/brush-smooth.md) | Smooth the terrain where you click. |
| [`/brush ex`](commands/brush-ex.md) | Put out fire where you click. |
| [`/brush gravity`](commands/brush-gravity.md) | Make blocks fall where you click. |
| [`/brush butcher`](commands/brush-butcher.md) | Kill mobs where you click. |
| [`/brush set`](commands/brush-set.md) | Fill a shape where you click. |
| [`/brush apply`](commands/brush-apply.md) | Apply a function to every block of a shape. |
| [`/brush paint`](commands/brush-paint.md) | Apply a function to the surfaces in a shape. |
| [`/brush forest`](commands/brush-forest.md) | Plant trees on the surfaces in a shape. |
| [`/brush deform`](commands/brush-deform.md) | Reshape terrain with a formula where you click. |
| [`/brush raise`](commands/brush-raise.md) | Raise terrain by one block where you click. |
| [`/brush lower`](commands/brush-lower.md) | Lower terrain by one block where you click. |
| [`/mask`](commands/brush-mask.md) | Limit the brush in your hand to a mask. |
| [`/mat`](commands/brush-material.md) | Change the material of the brush in your hand. |
| [`/range`](commands/brush-range.md) | Change how far the brush in your hand reaches. |
| [`/size`](commands/brush-size.md) | Change the size of the brush in your hand. |

### Navigation

| Command | What it does |
|---|---|
| [`/unstuck`](commands/unstuck.md) | Get out of a block you are stuck in. |
| [`/ascend`](commands/ascend.md) | Go up a floor. |
| [`/descend`](commands/descend.md) | Go down a floor. |
| [`/ceil`](commands/ceil.md) | Go up to the ceiling above you. |
| [`/up`](commands/up.md) | Go straight up. |
| [`/thru`](commands/thru.md) | Pass through the wall in front of you. |
| [`/jumpto`](commands/jumpto.md) | Teleport to the block you are looking at. |

### Biomes

| Command | What it does |
|---|---|
| [`/biomelist`](commands/biomelist.md) | List the biomes. |
| [`/biomeinfo`](commands/biomeinfo.md) | Show the biomes in your selection. |
| [`//setbiome`](commands/setbiome.md) | Change the biome of your selection. |

### Chunks

| Command | What it does |
|---|---|
| [`/chunkinfo`](commands/chunkinfo.md) | Show which chunk you are in. |
| [`/listchunks`](commands/listchunks.md) | List the chunks in your selection. |

### Snapshots

| Command | What it does |
|---|---|
| [`/snapshot`](commands/snapshot.md) | Choose a backup to restore from. |
| [`/snapshot list`](commands/snapshot-list.md) | List the available snapshots. |
| [`/snapshot use`](commands/snapshot-use.md) | Choose a snapshot by name. |
| [`/snapshot sel`](commands/snapshot-sel.md) | Choose a snapshot by its number in the list. |
| [`/snapshot before`](commands/snapshot-before.md) | Choose the last snapshot before a date. |
| [`/snapshot after`](commands/snapshot-after.md) | Choose the first snapshot after a date. |
| [`/restore`](commands/restore.md) | Restore your selection from a snapshot. |

### WorldEdit

| Command | What it does |
|---|---|
| [`/worldedit`](commands/worldedit.md) | Commands about WorldEdit itself. |
| [`/worldedit version`](commands/worldedit-version.md) | Show the version. |
| [`/worldedit reload`](commands/worldedit-reload.md) | Reload the configuration. |
| [`/worldedit reloadmappings`](commands/worldedit-reloadmappings.md) | Reload the rotation mappings. |
| [`/worldedit cui`](commands/worldedit-cui.md) | Complete the handshake with the client mod. |
| [`/worldedit tz`](commands/worldedit-tz.md) | Set your timezone. |
| [`/worldedit help`](commands/worldedit-help.md) | Show the commands and how to use them. |

