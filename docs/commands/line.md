# `//line`

Draw a line between the two corners of your selection.

Only works with a cuboid selection.

## Usage

```
//line [-h] <pattern> [<thickness>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to draw with. |
| `thickness` | How far to thicken the line. 0 is a line of single blocks, which is the default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make only the outer shell of a thick line. |

## Examples

| Command | What it does |
|---|---|
| `//line stone` | Draw a thin stone line. |
| `//line -h glass 3` | Draw a hollow glass tube. |

[All commands](../README.md)
