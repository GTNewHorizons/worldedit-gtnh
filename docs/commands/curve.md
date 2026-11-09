# `//curve`

Draw a smooth curve through your selected points.

Only works with a [convex selection](sel.md).

## Usage

```
//curve [-h] <pattern> [<thickness>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to draw with. |
| `thickness` | How far to thicken the curve. 0 is the default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make only the outer shell of a thick curve. |

## Examples

| Command | What it does |
|---|---|
| `//curve stone 1` | Draw a stone curve. |

[All commands](../README.md)
