# `//generatebiome`

Set the biome of your selection according to a formula.

Works like [//generate](generate.md), but sets a [biome](biomelist.md) wherever the formula is positive.

## Usage

```
//generatebiome [-hroc] <biome> <expression>
```

Also typed: `//gb`, `//genbiome`

## Arguments

| Argument | Meaning |
|---|---|
| `biome` | The name of the biome. |
| `expression` | The formula. It takes the rest of the line, so it may hold spaces. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-h` | Make the shape hollow. |
| `-r` | Use real world coordinates. |
| `-o` | Use coordinates relative to your [placement position](toggleplace.md). |
| `-c` | Use coordinates relative to the center of the selection, on the same scale as the blocks. |

## Examples

| Command | What it does |
|---|---|
| `//generatebiome plains x*x+z*z<1` | A round patch of plains. |

## Notes

- You may have to rejoin the game to see a biome change.

## Permission

`worldedit.generation.shape or worldedit.biome.set`

[All commands](../README.md)
