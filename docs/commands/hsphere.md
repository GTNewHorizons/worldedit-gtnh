# `//hsphere`

Generate a hollow sphere or ellipsoid.

The same as [//sphere](sphere.md) with `-h`.

## Usage

```
//hsphere <pattern> <radius> [<raised>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to build with. |
| `radius` | The radius. Three radii separated by commas make an ellipsoid, in the order north/south, up/down, east/west. |
| `raised` | `yes` to move the sphere up, so it rests on your position. `no` by default. |

## Examples

| Command | What it does |
|---|---|
| `//hsphere glass 10` | A glass ball. |

## Permission

`worldedit.generation.sphere`

[All commands](../README.md)
