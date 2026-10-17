# Controlify Chords

A standalone Fabric addon for Controlify that adds configurable ordered controller-button chords.

## Target compatibility

- Minecraft Java Edition 26.2
- Fabric Loader 0.19 or newer
- Controlify 3.5.3+mc26.2

## Status

Initial project setup. Input interception, configuration UI, and runtime behavior are not yet verified in-game.

## Intended behavior

- Ordered modifier sequences followed by a short trigger press.
- Releasing any required modifier cancels the pending chord.
- Only explicitly configured triggers participate in chord handling; other inputs retain normal behavior.
- Configurable actions and careful handling of overlapping ordinary bindings.

This project is independent of Controlify and does not redistribute Controlify.
