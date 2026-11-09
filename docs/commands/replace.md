# `//replace`

Replace blocks in your selection.

With one word, every block that is not air is replaced by it. With two words, the first says what to replace and the second what to replace it with.

## Usage

```
//replace [-f] <from-or-to> [<to>]
```

Also typed: `//re`, `//rep`

## Arguments

| Argument | Meaning |
|---|---|
| `from-or-to` | With a second word, the [mask](../masks.md) of what to replace. On its own, the [pattern](../patterns.md) to replace with. |
| `to` | The [pattern](../patterns.md) to replace with. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-f` | Accepted for compatibility, it has no effect. |

## Examples

| Command | What it does |
|---|---|
| `//replace glass` | Turn everything that is not air into glass. |
| `//replace stone dirt` | Turn stone into dirt. |
| `//replace stone,cobblestone 50%dirt,50%sand` | Replace two blocks with a mix. |

## Permission

`worldedit.region.replace`

[All commands](../README.md)
