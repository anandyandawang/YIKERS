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

## Build

```bash
./gradlew build
```

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
