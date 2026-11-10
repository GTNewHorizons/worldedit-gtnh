# `//sel`

Choose how you select, or clear your selection.

Without a type this clears your selection, which is what `//desel` is usually typed for.

## Usage

```
//sel [-d] [<type>]
```

Also typed: `//desel`, `//deselect`, `//;`

## Arguments

| Argument | Meaning |
|---|---|
| `type` | One of `cuboid` (two corners), `extend` (a corner, then click to grow), `poly` (a 2D polygon with a height), `ellipsoid`, `sphere`, `cyl` or `convex` (also `hull` and `polyhedron`). |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-d` | Also remember this type as your default for next time. |

## Examples

| Command | What it does |
|---|---|
| `//sel` | Clear the selection. |
| `//sel sphere` | Select with a center and a radius. |
| `//sel -d extend` | Use the extending selection from now on. |

[All commands](../README.md)
