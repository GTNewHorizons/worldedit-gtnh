# `//replacenear`

Replace blocks around you.

With one word after the size, every block that is not air is replaced by it. With two, the first says what to replace.

## Usage

```
//replacenear [-f] <size> <from-or-to> [<to>]
```

Also typed: `/replacenear`

## Arguments

| Argument | Meaning |
|---|---|
| `size` | How far to go, as a cube around you. |
| `from-or-to` | With a second word, the [blocks](../patterns.md#blocks) to replace, separated by commas. On its own, the [pattern](../patterns.md) to replace with. |
| `to` | The [pattern](../patterns.md) to replace with. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-f` | A block without a data value only matches data value 0, instead of all of them. |

## Examples

| Command | What it does |
|---|---|
| `//replacenear 10 stone dirt` | Turn nearby stone into dirt. |

[All commands](../README.md)
