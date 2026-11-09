# `//sphere`

Generate a filled sphere or ellipsoid.

Centered on your [placement position](toggleplace.md).

## Usage

```
//sphere [-h] <pattern> <radius> [<raised>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to build with. |
| `radius` | The radius. Three radii separated by commas make an ellipsoid, in the order north/south, up/down, east/west. |
| `raised` | `yes` to move the sphere up, so it rests on your position instead of being centered on it. `no` by default. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make it hollow. |

## Examples

| Command | What it does |
|---|---|
| `//sphere stone 5` | A stone ball of radius 5. |
| `//sphere -h glass 10 yes` | A glass dome shell standing on your position. |
| `//sphere stone 4,8,4` | A tall ellipsoid. |

## Permission

`worldedit.generation.sphere`

[All commands](../README.md)
