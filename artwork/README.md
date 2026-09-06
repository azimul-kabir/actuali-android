# Actua icon

`actua-icon.png` is the approved high-resolution Actua artwork.
`actua-play-store.png` is its 512 × 512 Play Store export.

The Android adaptive icon is split into background, foreground and monochrome
vector drawables under `app/src/main/res`. They reproduce the approved
ribbon-and-envelope silhouette while allowing Android to recolor the
monochrome layer. Keep the foreground inside the
adaptive-icon safe zone and preserve the single-color monochrome silhouette for
Android 13 and later themed icons.
