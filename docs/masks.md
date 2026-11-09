# Masks

A mask says which positions an edit may touch. You use one to say what to replace, as in [`//replace`](commands/replace.md), to limit all your edits with [`//gmask`](commands/gmask.md), or to limit a brush with [`/mask`](commands/brush-mask.md).

## What you can write

| You type | It matches |
|---|---|
| `stone` | Stone |
| `stone,dirt` | Stone or dirt |
| `wool:14` | Red wool only. Without a data value, a block matches all data values. |
| `!stone` | Everything except stone |
| `#existing` | Everything that is not air |
| `#solid` | Solid blocks |
| `#selection`, `#region` or `#sel` | Inside your selection as it is now |
| `#dselection`, `#dregion` or `#dsel` | Inside your selection, following it when it changes later |
| `>stone` | Directly above stone |
| `<stone` | Directly below stone |
| `$plains,desert` | In one of these biomes |
| `%25` | A random 25 percent of the positions |
| `=y<64` | Where the formula is true, see [expressions](expressions.md) |

`!` works in front of any other mask, so `!#existing` is air and `!$plains` is anything outside plains.

`>` and `<` on their own mean above and below any block that is not air. They do not match positions that are themselves that block.

## Combining masks

Separate masks with a space to require all of them. Because of the space, put the whole mask in quotes:

```
//gmask "stone >dirt"
//gmask "$plains %50"
```

[All commands](README.md)
