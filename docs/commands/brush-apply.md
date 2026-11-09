# `/brush apply`

Apply a function to every block of a shape.

## Usage

```
/brush apply <shape> <size> forest <tree>
/brush apply <shape> <size> item <item>
/brush apply <shape> <size> set <pattern>
```

## Arguments

| Argument | Meaning |
|---|---|
| `shape` | `cuboid`, `sphere` or `cyl`. |
| `size` | The size of the shape. |
| `tree` | With `forest`: the [tree type](../trees.md) to grow. |
| `item` | With `item`: the item to use on each block, such as `minecraft:dye:15` for bone meal. |
| `pattern` | With `set`: the [pattern](../patterns.md) to fill with. |

## Examples

| Command | What it does |
|---|---|
| `/br apply sphere 5 forest oak` | Grow oaks wherever they fit. |
| `/br apply sphere 5 item minecraft:dye:15` | Bone meal everything. |

[All commands](../README.md)
