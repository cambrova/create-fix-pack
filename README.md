# Create: Fix Pack

Fixes for long-standing Create bugs, each with its own switch, for NeoForge 1.21.1. Not affiliated with the Create team.

Each fix knows which Create issue it fixes. When a Create release fixes that bug itself, the fix switches off on that version and later.

| Switch | Create issue | What it fixes |
|---|---|---|
| `crafterKeepsOutput` | [#4510](https://github.com/Creators-of-Create/Create/issues/4510) | Mechanical crafters waiting to put their result into a full inventory dropped it on the ground when the world loaded. |
| `gaugeNoOverRequest` | [#9987](https://github.com/Creators-of-Create/Create/issues/9987) | Factory gauges ordered more than their target, because packagers counted items as arrived when some were taken out. |
| `gaugeSingleplayerCountOnce` (experimental, off) | #9987 | In singleplayer, packager arrivals were counted twice. |
| `gaugeForgetStockWhileBlind` (experimental, off) | #9987 | A packager that briefly couldn't see its inventory counted everything in it as new afterwards. |
| `linkKeepsWaitingLinks` (experimental, off) | [#5009](https://github.com/Creators-of-Create/Create/issues/5009) | Redstone links in a chunk that was briefly not fully loaded were dropped from their frequency for good, so receivers stopped following their transmitter. |

Experimental fixes have no passing test yet, so they start switched off. They get switched on once they are tested.

Config: `config/create_fix_pack.properties`.

## Tests

Every fix has a GameTest, and a control that runs the same setup with the fix off and has to reproduce the bug.

MIT.
