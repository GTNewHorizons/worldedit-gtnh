# Blocks and patterns

Many commands ask what to build with. Some want a single block, most accept a pattern.

## Blocks

A block is written by name or by ID, optionally with a data value after a colon.

| You type | It means |
|---|---|
| `stone` | Stone, by name |
| `1` | Stone, by ID |
| `wool:14` | Red wool: wool with data value 14 |
| `oak_stairs` | Underscores count as spaces in a name |
| `hand` | The block you are holding |
| `pos1` | The block at position 1 of your selection |

To find a name or ID, use [`//searchitem`](commands/searchitem.md), or bind the [`//tool info`](commands/tool-info.md) tool and click a block in the world.

## Patterns

A pattern is what to place at each position. The simplest pattern is one block, so anywhere a pattern is asked for you can just type `stone`.

**Random mix.** Separate blocks with commas. A number with `%` gives each one a weight:

```
//set stone,dirt
//set 70%stone,20%cobblestone,10%gravel
```

The weights do not need to add up to 100. A block without a weight counts as 1.

**The clipboard.** `#clipboard`, or `#copy`, repeats your clipboard:

```
//set #clipboard
```

## Where this is used

Commands name their argument `pattern` when they accept all of the above, and `block` when they want one block, for example [`//removenear`](commands/removenear.md).

[All commands](README.md)
