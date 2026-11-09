# `//generate`

Fill your selection according to a formula.

A block is placed wherever the formula is positive. By default the selection counts as going from -1 to 1 on each axis. See [expressions](../expressions.md).

## Usage

```
//generate [-hroc] <pattern> <expression>
```

Also typed: `//g`, `//gen`

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to build with. The formula can also set `type` and `data`. |
| `expression` | The formula. It takes the rest of the line, so it may hold spaces. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make the shape hollow. |
| `-r` | Use real world coordinates. |
| `-o` | Use coordinates relative to your [placement position](toggleplace.md). |
| `-c` | Use coordinates relative to the center of the selection, on the same scale as the blocks. |

## Examples

| Command | What it does |
|---|---|
| `//generate stone x*x+y*y+z*z<1` | A sphere that fills the selection. |
| `//generate -h glass x*x+z*z<1` | A hollow cylinder. |

## Permission

`worldedit.generation.shape`

[All commands](../README.md)
