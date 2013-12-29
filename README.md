# BlockSaver [![Travis CI](https://secure.travis-ci.org/Indiv0/block-saver.png)](http://travis-ci.org/#!/Indiv0/block-saver)

BlockSaver is a plugin for Bukkit which attempts to provide block protection through the reinforcement of certain blocks with others, similarly to Citadel.

## Administrators

### Download

You can find the latest builds of the plugin at my [ci](http://ci.nikitapek.in/job/block-saver/).
You can find stable downloads with detailed changelog information in the [files](http://dev.bukkit.org/bukkit-plugins/blocksaver/files/) section of the plugin's BukkitDev page.

### Installation

Simply drop the latest .jar into the /plugins directory of your server.

### Usage

Further plugin information can be found at the plugin's [BukkitDev](http://dev.bukkit.org/bukkit-plugins/blocksaver/) page.

## Developers

BlockSaver does not have a formal API at the moment, but you can download the latest version via maven by adding the following snippets to your plugin's pom.xml.

### Repository

    <repositories>
      <repository>
        <id>indiv0's Repo</id>
        <url>http://repo.nikitapek.in/maven/releases</url>
      </repository>
    </repositories>

### Dependency

    <dependency>
      <groupId>in.nikitapek</groupId>
      <artifactId>block-saver</artifactId>
      <version>1.16.0</version>
    </dependency>
