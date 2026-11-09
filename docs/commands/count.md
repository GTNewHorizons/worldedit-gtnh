# `//count`

Count a block in your selection.

## Usage

```
//count [-d] <block>
```

## Arguments

| Argument | Meaning |
|---|---|
| `block` | The [block](../patterns.md#blocks) to count, or several separated by commas. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-d` | Tell data values apart. Typing a data value, like `wool:14`, does this too. |

## Examples

| Command | What it does |
|---|---|
| `//count stone` | Count stone of any data value. |
| `//count wool:14,wool:11` | Count red and blue wool. |

## Permission

`worldedit.analysis.count`

[All commands](../README.md)
