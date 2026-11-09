# `//stack`

Repeat your selection.

Places copies of the selection next to it, in a row.

## Usage

```
//stack [-sa] [<count> [<direction>]]
```

## Arguments

| Argument | Meaning |
|---|---|
| `count` | How many copies. At least 1, and 1 by default. |
| `direction` | The [direction](../directions.md). By default, the direction you are looking in. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-s` | Move the selection to the last copy. |
| `-a` | Skip air blocks. |

## Examples

| Command | What it does |
|---|---|
| `//stack 5` | Make 5 copies towards where you look. |
| `//stack -a 3 up` | Make 3 copies upwards without pasting air. |

## Permission

`worldedit.region.stack`

[All commands](../README.md)
