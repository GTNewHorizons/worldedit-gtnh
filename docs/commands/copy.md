# `//copy`

Copy your selection to your clipboard.

Where you stand matters: the copy remembers its position relative to you, and [//paste](paste.md) places it relative to you in the same way.

## Usage

```
//copy [-e] [-m <mask>]
```

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-e` | Also copy the entities in the selection, such as mobs, item frames and minecarts. Players are never copied. |
| `-m` | Only copy what matches the [mask](../masks.md). The other blocks become air in the copy. |

## Examples

| Command | What it does |
|---|---|
| `//copy` | Copy the selection. |
| `//copy -m !air` | Copy everything that is not air. |
| `//copy -e` | Copy the selection with its entities. |

[All commands](../README.md)
