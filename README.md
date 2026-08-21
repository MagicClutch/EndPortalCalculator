# End Portal Calculator

Fabric mod for finding the stronghold from Eye of Ender throws. Throw a couple of eyes, it does the triangulation for you and points you at the result - no alt-tabbing to a browser calculator.

## What it does

- **Auto capture** - throw an eye normally, the mod watches it fly and land and turns that into a measurement by itself. No key to press.
- **F3+C capture** - if you'd rather do it manually (or auto mode isn't picking something up right), look at a landed eye and hit F3+C like you would in vanilla.
- **Least-squares fit** - with 2+ throws it calculates the most likely X/Z instead of just intersecting two lines, and starts rejecting statistical outliers once you've got 5+ throws in.
- **Accuracy readout** - shows how confident the current estimate is, and warns you if your throws are too bunched up together to give a reliable angle.
- **Too-close-throw handling** - a throw from nearly the same spot as an earlier one still gets counted (it's still real data), it just gets flagged and drawn in red instead of silently making your estimate worse.
- **Locator Bar waypoint** - the estimate shows up as a real waypoint dot in vanilla's own Locator Bar, with a "Stronghold" label above it, instead of some custom HUD marker bolted on top.
- **World beam** - a vertical line at the estimated location so you can see roughly where to dig even before you're close enough for the Locator Bar dot to line up.
- **In-game GUI** - press G (rebindable) to open a panel with your throw list, mode switch, waypoint toggle, minimum throw-distance setting, and a one-click "clear all".
- **`/strongholdtp`** - teleports you to the current estimate. Needs OP or creative, same as vanilla `/tp` would require anyway.
- **Per-world save** - throws are saved per world/server and reload automatically next time you join, so you don't lose progress between sessions.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3+
- Fabric API
- Fabric Language Kotlin

## Keybinds

| Action | Default key |
|---|---|
| Open GUI | G |
| Toggle HUD | unbound |
| Manual capture | F3+C (vanilla combo, not a rebindable key) |

All rebindable through Minecraft's normal controls menu.

## Notes

Client-side only - no server mod required, and nothing here touches or requires anything on the server beyond what vanilla `/tp` already needs for the teleport command. The Locator Bar waypoint is a synthetic client-only entry, never sent over the network, so nobody else can see it.

## License

MIT - see LICENSE.txt.
