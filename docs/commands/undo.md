# `//undo`

Undo your last edit.

Refused while an edit of yours is still running. Wait for it or use [//cancel](cancel.md).

## Usage

```
//undo [<times> [<player>]]
```

## Arguments

| Argument | Meaning |
|---|---|
| `times` | How many edits to undo. 1 by default. |
| `player` | The player whose history to undo. This needs the extra permission `worldedit.history.undo.other`. |

## Examples

| Command | What it does |
|---|---|
| `//undo` | Undo the last edit. |
| `//undo 3` | Undo the last three. |
| `//undo 1 Notch` | Undo the last edit of another player. |

[All commands](../README.md)
