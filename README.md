## Stoneworks HUD Chat

An unofficial Fabric client mod that displays your currently selected chat channel in your HUD. Designed for Stoneworks and other RedisChat-based servers.

### Features
- Current channel shown on-screen with colorized prefix
- In-game HUD config screen: drag to move, resize via handles, snap to edges/center
- Alignment modes: left-to-right, center, right-to-left
- Scale control with clamped min/max for readability
- Visibility toggle keybind
- Auto-detects server channel changes and updates the HUD
- Config persisted to `config/stoneworks_chat.json`

### Keybinds
- Open HUD Config: `H`
- Toggle HUD Visibility: `V`

You can change keybinds in Minecraft Controls.

### Config
Location: `config/stoneworks_chat.json`

- `currentChannel`: string key of the current channel
- `channels`: map of channel key -> object
  - `display`: string shown in the HUD prefix
  - `color`: one of `white|green|light_red|cyan|red|yellow`
  - `aliases`: array of strings (e.g., ["/g", "/global"]) used to detect channel switches
  - `uiName`: optional UI-friendly name
- `hudX`, `hudY`: alignment coordinates (depend on alignment mode)
- `textAlign`: `ltr|center|rtl`
- `hudScale`: float between 0.75 and 4.0
- `hudVisible`: boolean
- `hudAnchorX`: optional `LEFT|CENTER|RIGHT`
- `hudAnchorY`: optional `TOP|CENTER|BOTTOM`
- `hudOffsetX`, `hudOffsetY`: pixel offsets from the chosen anchors
- `showHudTutorial`: boolean

Defaults for Stoneworks channels are added automatically if missing.

### License
MIT. See `LICENSE.txt`.

