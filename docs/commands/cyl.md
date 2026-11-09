# `//cyl`

Generate a filled cylinder.

Starts at your [placement position](toggleplace.md) and goes up.

## Usage

```
//cyl [-h] <pattern> <radius> [<height>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to build with. |
| `radius` | The radius. Two radii separated by a comma make an elliptical cylinder, north/south first and east/west second. |
| `height` | The height. 1 by default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make it hollow. |

## Examples

| Command | What it does |
|---|---|
| `//cyl stone 5 10` | A stone pillar. |
| `//cyl -h stonebrick 8,12 5` | A hollow elliptical tower. |

## Permission

`worldedit.generation.cylinder`

[All commands](../README.md)
