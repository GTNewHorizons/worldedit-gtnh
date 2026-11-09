# `//contract`

Make your selection smaller.

Pulls one side of the selection in.

## Usage

```
//contract <amount> [<reverse-or-direction> [<direction>]]
```

## Arguments

| Argument | Meaning |
|---|---|
| `amount` | How many blocks to shrink by. |
| `reverse-or-direction` | Either how many blocks to also shrink from the opposite side, or the [direction](../directions.md). Without it, the direction you are looking in is used. |
| `direction` | The [direction](../directions.md), when you also gave a reverse amount. Several can be given with commas, like `north,east`. |

## Examples

| Command | What it does |
|---|---|
| `//contract 5` | Pull in the side you look at by 5 blocks. |
| `//contract 5 2 up` | Pull in the top by 5 and the bottom by 2. |

## Permission

`worldedit.selection.contract`

[All commands](../README.md)
