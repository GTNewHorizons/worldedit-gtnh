# `/brush paint`

Apply a function to the surfaces in a shape.

## Usage

```
/brush paint <shape> <size> <density> forest <tree>
/brush paint <shape> <size> <density> item <item>
/brush paint <shape> <size> <density> set <pattern>
```

## Arguments

| Argument | Meaning |
|---|---|
| `shape` | `cuboid`, `sphere` or `cyl`. |
| `size` | The size of the shape. |
| `density` | How much to cover, from 0 to 100. |
| `tree` | With `forest`: the [tree type](../trees.md) to grow. |
| `item` | With `item`: the item to use on each block. |
| `pattern` | With `set`: the [pattern](../patterns.md) to place. |

## Examples

| Command | What it does |
|---|---|
| `/br paint sphere 8 30 set grass` | Scatter grass on surfaces. |

## Permission

`worldedit.brush.paint`

[All commands](../README.md)
