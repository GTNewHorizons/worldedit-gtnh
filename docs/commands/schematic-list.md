# `/schematic list`

List the saved schematics.

Shows 9 per page, sorted by name unless you ask otherwise.

## Usage

```
/schematic list [-dn] [-p <page>]
```

Also typed: `/schematic all`, `/schematic ls`

## Flags

Flags go right after the command, before the arguments.

| Flag | Meaning |
|---|---|
| `-d` | Sort by date, oldest first. |
| `-n` | Sort by date, newest first. |
| `-p` | The page to show. |

## Examples

| Command | What it does |
|---|---|
| `/schem list` | The first page. |
| `/schem list -n -p 2` | The second page, newest first. |

## Permission

`worldedit.schematic.list`

[All commands](../README.md)
