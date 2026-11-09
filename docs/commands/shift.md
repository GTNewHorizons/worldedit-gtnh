# `//shift`

Move your selection without touching any blocks.

Only the selection moves. To move the blocks too, use [//move](move.md).

## Usage

```
//shift <amount> [<direction>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `amount` | How many blocks to move it. |
| `direction` | The [direction](../directions.md). Without it, the direction you are looking in is used. Several can be given with commas. |

## Examples

| Command | What it does |
|---|---|
| `//shift 10` | Move the selection 10 blocks towards where you look. |
| `//shift 5 up` | Move the selection 5 blocks up. |

## Permission

`worldedit.selection.shift`

[All commands](../README.md)
