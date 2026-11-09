# `/floodfill`

Bind the flood fill tool.

Clicking a block replaces it and all connected blocks of the same kind.

## Usage

```
/floodfill <pattern> <range>
```

Also typed: `/flood`

## Arguments

| Argument | Meaning |
|---|---|
| `pattern` | The [pattern](../patterns.md) to fill with. |
| `range` | How far to go. The server sets a maximum. |

## Examples

| Command | What it does |
|---|---|
| `/floodfill stone 10` | Flood fill with stone. |

## Permission

`worldedit.tool.flood-fill`

[All commands](../README.md)
