# Darktrack

A client-side Fabric mod that lets you control your music from inside Minecraft, so you never have to leave the game to pause or skip a song.

It works by sending Windows media keys, so it works with Spotify and most other music players, even while they run in the background.

## Features

- Play / pause with **F7**
- Next song with **F8**
- Previous song with **F6**
- Keys can be changed in Options > Controls > Key Binds under "Music Controls"
- Open a clickable music widget with **F9** (Prev, Play/Pause, Next buttons)

## Requirements

- Windows 10 or 11 (media keys are Windows-only for now)
- Minecraft 26.2
- Fabric Loader 0.20.1 or newer
- Fabric API

## Building from source

1. Install JDK 25
2. Clone this repo
3. Run `./gradlew build` (or `gradlew.bat build` on Windows)
4. The mod jar will be in `build/libs/`

To test in a development game, run the `runClient` Gradle task.

## Planned

- On-screen widget with clickable buttons
- Volume controls
- Show the current song title

## License

See the `LICENSE` file in this repo.