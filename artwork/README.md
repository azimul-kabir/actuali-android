# Actua icon

`actua-icon.svg` is the canonical, resolution-independent Actua artwork.
`actua-icon.png` is the high-resolution raster export used by the repository README.
`actua-play-store.png` is its 512 × 512 Play Store export.

The Android adaptive icon is split into background, foreground and monochrome
vector drawables under `app/src/main/res`. They reproduce the approved
three-dimensional ribbon-and-envelope mark on a solid background while allowing Android to recolor the
monochrome layer. Keep the foreground inside the
adaptive-icon safe zone and preserve the single-color monochrome silhouette for
Android 13 and later themed icons.
