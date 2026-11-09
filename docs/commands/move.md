# `//move`

Move the blocks in your selection.

## Usage

```
//move [-s] [<count> [<direction> [<leave-block>]]]
```

## Arguments

| Argument | Meaning |
|---|---|
| `count` | How many blocks to move by. At least 1, and 1 by default. |
| `direction` | The [direction](../directions.md). By default, the direction you are looking in. |
| `leave-block` | The [block](../patterns.md#blocks) to leave behind. Air by default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-s` | Move the selection along with the blocks. |

## Examples

| Command | What it does |
|---|---|
| `//move 10` | Move 10 blocks towards where you look. |
| `//move -s 5 up` | Move 5 blocks up and bring the selection along. |
| `//move 3 north stone` | Move 3 blocks north and leave stone behind. |

## Permission

`worldedit.region.move`

[All commands](../README.md)
