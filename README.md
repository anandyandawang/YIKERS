# YIKERS

libGDX game. Kotlin. Desktop (LWJGL3) backend.

## Layout

- `core/` — game logic, no backend. `YikersGame` lives here.
- `lwjgl3/` — desktop launcher + natives.

## Run

```bash
./gradlew lwjgl3:run
```

Move coral square with arrow keys. Esc/close window to quit.

## Build

```bash
./gradlew build
```

## Stack

- libGDX 1.13.1
- Kotlin 2.1.0
- JDK 21, Gradle 8.14.3
