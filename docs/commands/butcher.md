# `/butcher`

Kill mobs around you.

By default this only kills hostile mobs.

## Usage

```
/butcher [-plangbtfr] [<radius>]
```

## Arguments

| Argument | Meaning |
|---|---|
| `radius` | How far to go. -1 means all loaded chunks. Without it, the default of the server is used. |

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-p` | Also kill pets. |
| `-n` | Also kill NPCs. |
| `-g` | Also kill golems. |
| `-a` | Also kill animals. |
| `-b` | Also kill ambient mobs such as bats. |
| `-t` | Also kill mobs with a name tag. |
| `-f` | All of the above. |
| `-r` | Also destroy armor stands. |
| `-l` | Does nothing at the moment. |

## Examples

| Command | What it does |
|---|---|
| `/butcher 30` | Kill hostile mobs within 30 blocks. |
| `/butcher -a 30` | Animals too. |

## Permission

`worldedit.butcher, and a permission such as worldedit.butcher.pets for the flags`

[All commands](../README.md)
