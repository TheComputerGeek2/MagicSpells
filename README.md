<!--suppress HtmlDeprecatedAttribute -->
<div align="center">
    <br>
    <img src="https://files.jasperlorelai.eu/magicspells/images/readme_icon.png" alt="MagicSpells Icon">
    <hr>
    <h2><i>Magic without writing Java</i></h2>
    <a href="https://github.com/TheComputerGeek2/MagicSpells/actions"><img src="https://img.shields.io/github/actions/workflow/status/TheComputerGeek2/MagicSpells/build.yml?style=for-the-badge&logo=github" alt="Build Status"></a>
    <a href="https://discord.magicspells.dev"><img src="https://img.shields.io/discord/335237931633606656?color=5562e9&logo=discord&logoColor=white&style=for-the-badge" alt="Discord Server"></a>
    <a href="https://github.com/TheComputerGeek2/MagicSpells/releases"><img src="https://img.shields.io/github/downloads/TheComputerGeek2/MagicSpells/total.svg?style=for-the-badge&logo=github" alt="Github Downloads"></a>
    <img src="https://img.shields.io/github/license/TheComputerGeek2/MagicSpells?style=for-the-badge&logo=github" alt="Licence">
    <a href="https://bstats.org/plugin/bukkit/MagicSpells/892"><img src="https://img.shields.io/bstats/servers/892?style=for-the-badge" alt="In MC servers"></a>
</div>

[//]: # (These links are here for easier hyperlink referencing and less clutter in the actual text below.)
[Discord server]: https://discord.magicspells.dev
[resources channel]: https://canary.discord.com/channels/335237931633606656/1062270620911550524
[workshop channel]: https://canary.discord.com/channels/335237931633606656/1020045264343552010
[Releases]: https://github.com/TheComputerGeek2/MagicSpells/releases
[Wiki]: https://github.com/TheComputerGeek2/MagicSpells/wiki
[SpellRepo]: https://github.com/niblexis/ms-examples
[PAPIExp]: https://github.com/JasperLorelai/Expansion-MagicSpells
[Soundboard]: https://jasperlorelai.eu/soundboard/
[Nisovin]: https://nisovin.com/
[Bukkit]: https://dev.bukkit.org/projects/magicspells
[Spigot]: https://www.spigotmc.org/resources/magicspells.60847/
[PaperMC]: https://papermc.io/
[Modrinth]: https://modrinth.com/plugin/magicspells
[Hangar]: https://hangar.papermc.io/MagicSpells/MagicSpells
[DiscordBadge]: https://img.shields.io/badge/Join%20our%20Discord-blue?style=for-the-badge&color=586ff2
[WelcomeChannel]: https://files.jasperlorelai.eu/magicspells/images/readme_discord_screenshot.png

MagicSpells is a [PaperMC] plugin which gives its users the ability to modify their Minecraft servers by configuring existing features without writing Java code. It provides you with the tools for playing with blocks of logic, bringing other plugins together, playing fantastic special effects, making your own new mechanics, and more.

Check out more examples of what this plugin can do in our [Discord server] (in the [resources channel] and [workshop channel]).

---
## Resources 📝
 
* [Plugin Releases (Downloads)][Releases]
* [Community Wiki][Wiki]
* [Discord server]
* [Niblexis' Public Spell Repo][SpellRepo]
* [MagicSpells PAPI Expansion][PAPIExp]
* [JasperLorelai's Soundboard][Soundboard]



---
## Classical MagicSpells ⭐

This plugin was originally created by [Nisovin], and published on [Bukkit]. After some time TheComputerGeek2 took over the project and published it on [Spigot]. Since then, we dropped support for Spigot and moved to [PaperMC], and the plugin was published on [Modrinth] and [Hangar].



---
## Support 📞

If you need help with the plugin, our Discord server can provide you community support. You can also post suggestions there or find more resources that can be useful for you.

[![Join our Discord][DiscordBadge]][Discord server]

![Screenshot of the server][WelcomeChannel]



---
## Building 🧱

The move to Gradle has made building much easier. After cloning and navigating to the source's directory, you can simply run this command:
```
./gradlew build
```
The plugin jar will be in the `core/build/libs` directory.



---
## License 📜

MagicSpells is licensed under **GNU General Public License v3**. For details, see `LICENSE` file in the root directory.
- If your software is distributed and links to MagicSpells API, it must be licensed by a [GPL compatible license](https://www.gnu.org/licenses/license-list.html#GPLCompatibleLicenses).
- If your software modifies MagicSpells, you must also include the licenses of our dependencies linked in our `NOTICE` file.


---
## Dependency 📚

#### Gradle:

```groovy
repositories {
    maven {url "https://jitpack.io"}
}

dependencies {
    compileOnly("com.github.TheComputerGeek2.MagicSpells:core:main-SNAPSHOT")
}
```


#### Maven:

```xml
<repository>
    <id>jitpack-repo</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.TheComputerGeek2.MagicSpells</groupId>
    <artifactId>core</artifactId>
    <version>main-SNAPSHOT</version>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
