# `/brush deform`

Reshape terrain with a formula where you click.

See [expressions](../expressions.md).

## Usage

```
/brush deform [-ro] <shape> <size> <expression>
```

## Arguments

| Argument | Meaning |
|---|---|
| `shape` | `cuboid`, `sphere` or `cyl`. |
| `size` | The size of the shape. |
| `expression` | The formula, as one word without spaces. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-r` | Use real world coordinates. |
| `-o` | Use coordinates relative to your [placement position](toggleplace.md). |

## Examples

| Command | What it does |
|---|---|
| `/br deform sphere 8 y-=0.2` | Pull terrain up a little. |

[All commands](../README.md)
