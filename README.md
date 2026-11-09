# ![WorldEdit](http://static.sk89q.com/readme/worldedit.png)

[WorldEdit](https://github.com/EngineHub/WorldEdit/tree/forge-archive/1.7.10) fork at 1.7.10 for use in a GTNH instance. Non-forge
related code has been removed for ease of maintenance.

Requires [GTNHLib](https://github.com/GTNewHorizons/GTNHLib).

## Commands

See the [command documentation](docs/README.md) for everything you can type.

## Configuration

Settings are in `config/worldedit/worldedit.cfg`, see [configuration](docs/configuration.md). An old
`worldedit.properties` is imported once on first start.

## Adding or changing a command

Commands are [Brigadier](https://github.com/Mojang/brigadier) trees, registered through GTNHLib. Each group in
`com.sk89q.worldedit.command` declares its whole tree in a `register` method, with the code of the commands below it.
The helpers are in `com.sk89q.worldedit.command.brigadier`.

Every command needs a page in `docs/commands`. `CommandDocsTest` fails if a command has no page, or if its page does not
show its real usage.
