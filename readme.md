![Logo](https://raw.githubusercontent.com/ChiselsAndBits/Assets/main/Logos/logo.png "Logo")

| [Discord](https://discord.chisels-and-bits.com) | [Wiki](https://wiki.chisels-and-bits.com)  | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/chisels-bits)  | [Releases](https://www.curseforge.com/minecraft/mc-mods/chisels-bits/files) | [Code](https://github.com/ChiselsAndBits/Chisels-and-Bits/) | [Issues](https://github.com/ChiselsAndBits/Chisels-and-Bits/issues) | [Pull Requests](https://github.com/ChiselsAndBits/Chisels-and-Bits/pulls) | [Patreon](https://www.patreon.com/chisels_and_bits) |

### Minecraft mod:
Chisels and Bits is a Minecraft Mod which allows you to cut up the large blocks that make up your world, and place the little cuts back down.
In any way you like.

You want smoother stairs? Go for it.
You want better looking roofs? Or Pillars? It is all possible now.

You can make entirely new looking and feeling blocks by mixing and matching the bits of different already existing blocks.

### Compatibility:
#### Vanilla:
By default, all solid, simple, full cube blocks, like logs, planks, ores, storage blocks (diamond, emerald, iron etc) are supported.
Additionally, some specific blocks are curated by hand, this is especially done for all glass blocks.

#### Mod:
The scan for compatible blocks automatically also detects all mod blocks which are compatible.
If a mod maker wants to control the selection procedure for his or her blocks, he can do so, by adding them to either one of two tags:
- `chiselsandbits:chiselable/forced` to force the block to be chiselable (handy for glass like blocks)
- `chiselsandbits:chiselable/blocked` to prevent a block that is normally chiselable to be chiseled.

### Cross Platform Compatibility:
This mod uses [Scena](https://github.com/Communi-Suggestu/Scena) to run common code on both forge and fabric.
This means that the mod is compatible with both neoforge and fabric, and that the code is shared between both platforms via the core subproject.