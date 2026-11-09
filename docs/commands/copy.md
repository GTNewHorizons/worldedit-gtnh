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
| `-e` | Accepted, but entities are not copied at the moment. |
| `-m` | Only copy what matches the [mask](../masks.md). The other blocks become air in the copy. |

## Examples

| Command | What it does |
|---|---|
| `//copy` | Copy the selection. |
| `//copy -m !air` | Copy everything that is not air. |

## Permission

`worldedit.clipboard.copy`

[All commands](../README.md)
