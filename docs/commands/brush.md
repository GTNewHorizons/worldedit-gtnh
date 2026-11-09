# `/brush`

Bind a brush to the item in your hand.

A brush edits at a distance: right click with the item and it applies where you look. You can also type `//br`. Change a brush afterwards with [/mask](brush-mask.md), [/mat](brush-material.md), [/range](brush-range.md) and [/size](brush-size.md). Unbind it with [/none](none.md).

## Usage

```
/brush sphere [-h] <pattern> [<radius>]
/brush cylinder [-h] <pattern> [<radius> [<height>]]
/brush clipboard [-ap]
/brush smooth [-n] [<size> [<iterations>]]
/brush ex [<radius>]
/brush gravity [-h] [<radius>]
/brush butcher [-plangbtfr] [<radius>]
/brush set <shape> <size> <pattern>
/brush apply <shape> <size> forest <tree>
/brush apply <shape> <size> item <item>
/brush apply <shape> <size> set <pattern>
/brush paint <shape> <size> <density> forest <tree>
/brush paint <shape> <size> <density> item <item>
/brush paint <shape> <size> <density> set <pattern>
/brush forest <shape> <size> <density> <tree>
/brush deform [-ro] <shape> <size> <expression>
/brush raise <shape> <size>
/brush lower <shape> <size>
```

Also typed: `/br`

## Commands

- [`/brush sphere`](brush-sphere.md): Paint spheres.
- [`/brush cylinder`](brush-cylinder.md): Paint cylinders.
- [`/brush clipboard`](brush-clipboard.md): Paste your clipboard where you click.
- [`/brush smooth`](brush-smooth.md): Smooth the terrain where you click.
- [`/brush ex`](brush-ex.md): Put out fire where you click.
- [`/brush gravity`](brush-gravity.md): Make blocks fall where you click.
- [`/brush butcher`](brush-butcher.md): Kill mobs where you click.
- [`/brush set`](brush-set.md): Fill a shape where you click.
- [`/brush apply`](brush-apply.md): Apply a function to every block of a shape.
- [`/brush paint`](brush-paint.md): Apply a function to the surfaces in a shape.
- [`/brush forest`](brush-forest.md): Plant trees on the surfaces in a shape.
- [`/brush deform`](brush-deform.md): Reshape terrain with a formula where you click.
- [`/brush raise`](brush-raise.md): Raise terrain by one block where you click.
- [`/brush lower`](brush-lower.md): Lower terrain by one block where you click.
- [`/mask`](brush-mask.md): Limit the brush in your hand to a mask.
- [`/mat`](brush-material.md): Change the material of the brush in your hand.
- [`/range`](brush-range.md): Change how far the brush in your hand reaches.
- [`/size`](brush-size.md): Change the size of the brush in your hand.

[All commands](../README.md)
