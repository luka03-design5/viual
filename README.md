# Axiom Visuals (Fabric 1.21.11)

Visuals client + Spotify Lyrics.

## Controls
- **Right Shift** — ClickGUI (Hitboxes, BlockOverlay, StorageESP, Tracers, Fullbright)
- **K** — Lyrics menu

## Build
Push to GitHub → Actions → artifact `spotify-lyrics-jar` (or rename in workflow).
Needs Fabric Loader + Fabric API for 1.21.11.

## Modules
- **Hitboxes** — entity boxes (players red, others cyan)
- **BlockOverlay** — highlight targeted block
- **StorageESP** — chests, barrels, ender chests, shulkers
- **Tracers** — lines to players
- **Fullbright** — max gamma

Lyrics keep Poppins fonts (non-pixel UI). ClickGUI uses the same TTF.
