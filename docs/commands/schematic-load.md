# `//schematic load`

Load a schematic into your clipboard.

Also typed `//load`. Paste it afterwards with [//paste](paste.md).

Block and item IDs are matched by registry name, so a schematic saved in another pack or version loads with the right blocks. Blocks and items that don't exist in this game are left out (blocks become air), and you get a message listing them.

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
