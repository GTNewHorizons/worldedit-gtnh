# `//schematic load`

Load a schematic into your clipboard.

Also typed `//load`. Paste it afterwards with [//paste](paste.md).

Block and item IDs are matched by registry name, so a schematic saved in another pack or version loads with the right blocks, and with the right items in containers and on entities. Blocks that don't exist in this game become air, and stacks of missing items are removed from inventories. Elsewhere, such as in AE2 patterns, a missing item becomes empty in place so that nothing shifts. You get a message listing what was missing.

## Usage

```
//schematic load <filename-or-format> [<filename>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `filename-or-format` | With one word, the file name. With two, the [format](schematic-formats.md). |
| `filename` | The file name, when you gave a format. |

## Examples

| Command | What it does |
|---|---|
| `//schem load house` | Load `house.schematic`. |
| `//schem load mcedit house` | Load it with an explicit format. |

[All commands](../README.md)
