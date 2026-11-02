# timeo

An Android app for tracking how long you spend on the things in your life. You get a stopwatch per
category, subtimers that roll up into their parent, daily and weekly goals, and a GitHub-style heatmap
of the last 12 months. The visual style follows aryanashta.me: paper/ink palette, flat colour "plates",
Archivo and JetBrains Mono.

## Build

Requires JDK 17 and an Android SDK with platform 35 (`local.properties` → `sdk.dir`).

```bash
./gradlew testDebugUnitTest   # stats logic tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Using it

- **Today**: tap a plate to start or stop its stopwatch, and long-press it to edit. Use `+ sub` / `n sub` to add or show subtimers.
- **Insights**: pick a category (and optionally a subtimer) to see its heatmap, streaks, weekly goal and last 7 days.
  Tap a heatmap square, then *Sessions*, to view, edit or add sessions for that day.
- Debug builds offer **Load sample data** on the empty state.

Code layout: `data/` (Room entities, DAO, repository), `domain/StatsCalculator.kt` (rollups, midnight
splitting, streaks, heat levels), `ui/` (Compose screens and theme).
