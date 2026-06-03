# YIKERS

Roguelike-flavored vertical climber. Inspired by YIKES. libGDX + Kotlin,
desktop (LWJGL3). Built on a Fleks ECS + Box2D core.

Climb forever: jump up the gap-platforms, slide left/right to line up the
holes, dodge rolling boulders. Camera scrolls up and speeds up — fall below
it and you die. Score = platforms climbed.

## Controls

- Left / Right — slide (momentum, like the old tilt feel)
- Space / Up / click — jump (only when standing on something)
- Space / click on game-over — back to menu

## Run

```bash
./gradlew lwjgl3:run
```

## Bots / roster

A run holds X human climbers + Y bot climbers, each a distinct color. Counts come
from env vars (or `-Dyikers.*` system properties); default is one human, so
normal play is unchanged:

```bash
YIKERS_HUMANS=0 YIKERS_BOTS=1 ./gradlew lwjgl3:run   # one bot plays itself
YIKERS_HUMANS=1 YIKERS_BOTS=1 ./gradlew lwjgl3:run   # you + a bot
YIKERS_SEED=42 ./gradlew lwjgl3:run                  # reproducible layout
```

With 0 humans the menu auto-starts and the run auto-restarts on death (hands-off
attract mode) — which is what lets a bot run be screen-recorded. Bots steer
toward the next gap and hop; boulder-dodging is a TODO. Each climber dies on its
own; the run ends once all are dead.

## Build

```bash
./gradlew build
```

## Android

Tilt the phone left/right to slide, tap to jump (same shared controls as iOS).

Build an installable debug APK (signed with the local debug key):

```bash
./gradlew :android:assembleDebug
```

Output: `android/build/outputs/apk/debug/android-debug.apk`. Copy it to your
phone and open it — the first time, enable "install unknown apps" for the app
you opened it with (browser/file manager).

The `:android` module is included only when `local.properties` declares
`sdk.dir` — Android Studio writes it on import, or create it once with
`echo "sdk.dir=$ANDROID_HOME" > local.properties` (needs the Android SDK:
platform 35, build-tools 35.0.0). Gating on `sdk.dir` keeps the desktop, iOS,
and headless-test builds free of Android tooling.

### APK from CI

The **Android APK** workflow (`.github/workflows/android-apk.yml`) builds the
debug APK on every push and on manual dispatch, uploading it as the
`yikers-debug-apk` artifact — download it from the run's Artifacts section, no
local SDK required.

## Layout

- `core/` — game logic, no backend.
  - `screen/` — `MenuScreen`, `PlayScreen` (run lifecycle: builds the worlds).
  - `ecs/component`, `ecs/system`, `ecs/resource` — the Fleks ECS.
  - `ecs/EntityFactory.kt` — Box2D body + entity wiring.
  - `physics/` — Box2D contact listener.
  - `config/` — `GameConfig` (fixed consts), `RunConfig` (per-run knobs),
    `Prefs` (save keys).
- `lwjgl3/` — desktop launcher + natives.

## Architecture

- ktx `KtxGame` / `KtxScreen` for app flow (menu vs play).
- Inside `PlayScreen`: one Fleks `World` runs the sim each frame; one Box2D
  `World` does physics. A `Physics` component links each entity to its body;
  systems step physics, sync transforms, score, scroll, and render shapes.
- Asset-free: entities drawn as shapes (ShapeRenderer) + built-in font. No
  binary assets.

### Roguelike seams (built, not yet filled)

- `RunConfig` — mutable per-run knobs every system reads; items / events /
  characters tweak it live.
- `Lethal` marker — anything lethal kills the player; new enemies just add it.
- `Prefs` — reserved keys for unlocked characters + checkpoints.

Planned: procedural floors/biomes, per-run item builds, events that inject
run modifiers, varied enemies, characters unlocked at checkpoints.

## Stack

- libGDX 1.13.1
- Fleks 2.14 (ECS), ktx 1.13.1-rc1
- Kotlin 2.3.21
- JDK 21, Gradle 8.14.3
- Android Gradle Plugin 8.7.3 (android module)
