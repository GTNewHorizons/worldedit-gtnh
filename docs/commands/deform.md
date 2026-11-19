# `//deform`

Reshape your selection with a formula.

For each block the formula changes `x`, `y` and `z` to point at the block to fetch. By default the selection counts as going from -1 to 1 on each axis. See [expressions](../expressions.md).

## Usage

```
//deform [-ro] <expression>
```

## Arguments

| Argument | Meaning |
|---|---|
| `expression` | The formula. It takes the rest of the line, so it may hold spaces. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-r` | Use real world coordinates. |
| `-o` | Use coordinates relative to your [placement position](toggleplace.md). |

## Examples

| Command | What it does |
|---|---|
| `//deform y-=0.2` | Shift the contents of the selection. |
| `//deform -r y=y+sin(x/10)*3` | Add a wave along the x axis. |

[All commands](../README.md)
