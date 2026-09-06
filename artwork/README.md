# Actua icon

`actua-icon.png` is the canonical high-resolution Actua artwork and is used by the repository README.
`actua-play-store.png` is its 512 × 512 Play Store export.
`actua-icon.svg` is retained as an editable approximation, but it is not the
launcher source because its geometry differs from the approved mark.

The Android adaptive icon is split into background, foreground and monochrome
drawables under `app/src/main/res`. The foreground references the exact approved
transparent mark from `drawable-nodpi/actua_mark.png`; the background remains a
solid violet drawable. Android uses the same mark's alpha channel for the
monochrome layer, allowing Material You to recolor it on Android 13 and later.
Keep the foreground inside the adaptive-icon safe zone.
