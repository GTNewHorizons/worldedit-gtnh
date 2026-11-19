# `//wand`

Get the selection wand, or the navigation wand.

Gives you the selection wand, a wooden axe named "WorldEdit Wand" unless the server changed the item. Left click selects position 1, right click selects position 2.

With `-n`, gives you the navigation wand instead, a compass unless the server changed it. Left click jumps to the block you look at, right click passes through walls.

Only wands given by this command work, so ordinary wooden axes and compasses behave normally. Servers can turn this off with `requireWandTag`, so that any item of the wand's type works. Either way, using a wand needs permission: `worldedit.selection.pos` for the selection wand, and `worldedit.navigation.jumpto.tool` and `worldedit.navigation.thru.tool` for the navigation wand. Setting a wand's item to 0 in the configuration turns it off.

## Usage

```
//wand [-n]
```

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-n` | Give the navigation wand. |

## Examples

| Command | What it does |
|---|---|
| `//wand` | Get the selection wand. |
| `//wand -n` | Get the navigation wand. |

[All commands](../README.md)
