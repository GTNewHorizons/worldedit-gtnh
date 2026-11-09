# `//chunk`

Select a whole chunk.

Selects the full height of the chunk you stand in, or of the coordinates you type.

## Usage

```
//chunk [-sc] [<coordinates>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `coordinates` | A position as `x,z`, without spaces. It is read as block coordinates unless you use `-c`. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-s` | Grow your current selection so that it covers every chunk it touches. |
| `-c` | Read the coordinates as chunk coordinates. The block position `5,5` is chunk `0,0`. |

## Examples

| Command | What it does |
|---|---|
| `//chunk` | Select the chunk you stand in. |
| `//chunk -c 3,-2` | Select chunk 3,-2. |
| `//chunk -s` | Grow the selection to whole chunks. |

[All commands](../README.md)
