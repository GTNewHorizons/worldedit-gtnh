# `//cut`

Cut your selection to your clipboard.

Like [//copy](copy.md), but removes the blocks from the world.

## Usage

```
//cut [-e] [-m <mask>] [<leave-pattern>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `leave-pattern` | The [pattern](../patterns.md) to leave behind. Air by default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-e` | Accepted, but entities are not copied at the moment. |
| `-m` | Only cut what matches the [mask](../masks.md). Other blocks stay in the world, and are air in the clipboard. |

## Examples

| Command | What it does |
|---|---|
| `//cut` | Cut the selection. |
| `//cut dirt` | Cut and leave dirt behind. |

## Permission

`worldedit.clipboard.cut`

[All commands](../README.md)
