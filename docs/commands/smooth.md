# `//smooth`

Smooth the terrain in your selection.

## Usage

```
//smooth [-n] [<iterations>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `iterations` | How many times to smooth. More is smoother and slower. The default is 1. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-n` | Only look at naturally occurring blocks, so buildings and trees do not count as terrain. |

## Examples

| Command | What it does |
|---|---|
| `//smooth 3` | Smooth three times. |
| `//smooth -n` | Smooth without being disturbed by what stands on the terrain. |

[All commands](../README.md)
