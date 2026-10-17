# Create: Fix Pack

Fixes for long-standing Create bugs, each with its own switch, for NeoForge 1.21.1. Not affiliated with the Create team.

Each fix knows which Create issue it fixes. When a Create release fixes that bug itself, the fix switches off on that version and later.

| Switch | Create issue | What it fixes |
|---|---|---|
| `crafterKeepsOutput` | [#4510](https://github.com/Creators-of-Create/Create/issues/4510) | Mechanical crafters waiting to put their result into a full inventory dropped it on the ground when the world loaded. |

Config: `config/create_fix_pack.properties`.

## Tests

Every fix has a GameTest, and a control that runs the same setup with the fix off and has to reproduce the bug.

MIT.
