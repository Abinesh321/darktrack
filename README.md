
## Download

Get the latest jar from the [Releases page](https://github.com/Abinesh321/darktrack/releases).
OR [click](https://github.com/Abinesh321/darktrack/releases/download/v1.0.0/darktrack-1.0.0.jar) this for direct download

## Installation

1. Install Minecraft 26.2 with Fabric Loader
2. Download Fabric API for 26.2 and the Darktrack jar
3. Put both jars in your `mods` folder (`%appdata%\.minecraft\mods`)
4. Launch the Fabric profile

# Darktrack

A client-side Fabric mod that lets you control your music from inside Minecraft, so you never have to leave the game to pause, skip, or change the volume.

It works by sending Windows media keys, so it works with Spotify and most other music players, even while they run in the background.

## Features

- Music keys: play/pause (**F7**), next (**F8**), previous (**F6**)
- Volume keys: volume down (**[**), volume up (**]**), mute (**\\**)
- Clickable music widget with buttons, opened with **F9** (close it with **Esc**)
- All keys can be changed in Options > Controls > Key Binds under "Music Controls"

Note: the volume and mute controls change the Windows system volume, so Minecraft's sounds and other apps are affected too.

## Requirements

- Windows 10 or 11 (media keys are Windows-only for now)
- Minecraft 26.2
- Fabric Loader 0.20.1 or newer
- Fabric API

## Building from source

1. Install JDK 25
2. Clone this repo
3. Run `gradlew.bat build` on Windows (or `./gradlew build` on other systems)
4. The mod jar will be in `build/libs/`

To test in a development game, run the `runClient` Gradle task.

## Planned

- Button in the pause menu that opens the widget
- Always-visible mini HUD
- Show the current song title

## License

See the `LICENSE` file in this repo.