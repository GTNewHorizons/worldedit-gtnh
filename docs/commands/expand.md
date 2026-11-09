# `//expand`

Make your selection bigger.

Pushes the selection out in a direction. With `vert` or `vertical` it stretches from the bottom to the top of the world.

## Usage

```
//expand vert
//expand vertical
//expand <amount> [<reverse-or-direction> [<direction>]]
```

## Arguments

| Argument | Meaning |
|---|---|
| `amount` | How many blocks to grow by. |
| `reverse-or-direction` | Either how many blocks to also grow in the opposite direction, or the [direction](../directions.md). Without it, the direction you are looking in is used. |
| `direction` | The [direction](../directions.md), when you also gave a reverse amount. Several can be given with commas, like `north,east`. |

## Examples

| Command | What it does |
|---|---|
| `//expand 10` | Grow 10 blocks towards where you look. |
| `//expand 10 up` | Grow 10 blocks upwards. |
| `//expand 10 5 north` | Grow 10 blocks north and 5 blocks south. |
| `//expand vert` | Stretch from bedrock to the top of the world. |

## Permission

`worldedit.selection.expand`

[All commands](../README.md)
