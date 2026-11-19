# Expressions

Some commands take a math formula: [`//calc`](commands/calc.md), [`//generate`](commands/generate.md), [`//generatebiome`](commands/generatebiome.md), [`//deform`](commands/deform.md), the [deform brush](commands/brush-deform.md) and the `=` [mask](masks.md). Any argument that asks for a number also takes one, like `//stack 2*8`.

## Writing a formula

The usual operators work: `+ - * / %`, `^` for powers, comparisons such as `<`, `>=`, `==` and `!=`, and `&&`, `||` and `!` for logic. Assigning with `=` and `-=` changes a variable, and `;` separates steps.

```
//calc (2 + 3) * 4
//generate stone y < sin(x * 3) / 2
//deform y -= 0.2
```

## Variables

| Command | Variables |
|---|---|
| `//generate`, `//generatebiome` | Read `x`, `y` and `z`. `//generate` can also set `type` and `data` to choose the block. |
| `//deform` | Change `x`, `y` and `z` to point at the block to fetch. |
| `=` mask | Read `x`, `y` and `z`. |

`//generate` and `//deform` treat the selection as going from -1 to 1 on each axis, unless you use one of their flags for real coordinates.

## Functions

| Group | Functions |
|---|---|
| Trigonometry | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `sinh`, `cosh`, `tanh` |
| Roots and logarithms | `sqrt`, `cbrt`, `exp`, `ln`, `log`, `log10` |
| Rounding | `abs`, `ceil`, `floor`, `rint`, `round` |
| Comparing | `min`, `max` |
| Random and noise | `random`, `randint`, `perlin`, `voronoi`, `ridgedmulti` |
| The world | `query`, reads the block at a position |
| Other | `rotate`, `swap`, `megabuf`, `gmegabuf`, `closest`, `gclosest` |

[All commands](README.md)
