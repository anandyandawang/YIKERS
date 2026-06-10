# YIKERS architecture

map of the machine. read this before adding feature. complexity very very bad —
each piece below own ONE job. keep it that way.

## modules

```
shared        wire types + configs + boot knobs. NO engine imports (no gdx/fleks/ktx). pure data.
server        authoritative sim: Fleks ECS + Box2D + 60Hz DedicatedServer.
client-shared session abstractions (GameSession, InputAgent, Participant). engine-free.
client        libGDX frontend: screens + SnapshotRenderer. draws snapshots, owns no sim.
bot           autopilot InputAgent (BotBrain). engine-free, sees only WorldSnapshot.
e2e           real-socket tests: server + clients over TCP.
arch          Konsist rules + checkModuleDeps enforce all the above. break rule = red CI.
lwjgl3/ios/android  thin launchers.
```

rule of thumb: game truth live in `server`. client and bot only ever see
`WorldSnapshot`. anything both sides need go in `shared` and stay engine-free.

## tick pipeline (server)

`DedicatedServer` tick 60Hz: drain inputs -> `GameInstance.tick(dt)` -> broadcast
snapshot. inside tick, Fleks systems run in THIS order. order is load-bearing.
the order lives in ONE place: `sim/SimWorld.kt` (`buildSimWorld`) — production
`GameInstance` and the test harness `buildSim` both assemble from it, so add a
system there and every consumer gets it.

| # | system | job |
|---|--------|-----|
| 1 | `ControlSystem` | controller (relay/bot/script) fills `Intent` |
| 2 | `MoveSystem` | `Intent.vx` -> body velocity |
| 3 | `JumpSystem` | `Intent.jump` + `Augments` traits -> jump |
| 4 | `WallFollowSystem` | walls track `scrollY` |
| 5 | `PhysicsStepSystem` | Box2D sub-steps at 1/300. contact listener emits events HERE |
| 6 | `TransformSyncSystem` | body -> `Transform` |
| 7 | `BoulderSystem` | boulders bounce off walls |
| 8 | `PlatformScoreSystem` | primary passes slab -> score, emit `PlatformCleared` |
| 9 | `PlatformBridgeSystem` | all climbers landed + above -> seal hole |
| 10 | `PlatformRecycleSystem` | slab under kill-line -> fresh slab via `LevelGenerator`, maybe boulder, emit `PlatformRecycled` |
| 11 | `ScrollSystem` | kill-line rises, accelerates |
| 12 | `DeathSystem` | kill-line / `LethalContact` -> `Dead`, emit `PlayerDied`, `RunEnded` |
| 13 | `EventFlushSystem` | clear event queue. MUST stay last |

## events (`com.yikers.ecs.event`)

`Events` resource = per-tick queue. emit anywhere (system or contact listener);
any LATER system same tick reads with `events.each<T> { }`; flushed at tick end.
nothing crosses ticks. this the trigger seam for roguelike logic: "on platform
cleared give shiny rock", "on death drop loot" — write a system, place it after
the emitter, before flush.

current events: `LethalContact`, `PlatformCleared`, `PlatformRecycled`,
`PlayerDied`, `RunEnded`.

## level generation (`com.yikers.level`)

ALL layout randomness behind `LevelGenerator`:

- `nextPlatform(y)` -> hole position/width
- `boulderOnRecycle(y)` -> boulder or null

`ClassicGenerator` = current game. new biome / difficulty curve / floor theme =
new implementation, swap in `GameInstance`. sim systems never roll layout dice
themselves. generators draw from `MathUtils.random` so `SessionConfig.seed`
reproduces the run.

## resources (injected into Fleks world)

- `RunConfig` (shared, mutable) — feel knobs. roguelike items/curses mutate live.
- `RunState` — score, scrollY, dead, totalTime.
- `Refs` — primary player + boulder pool.
- `Arena` — ground + walls (static, not entities).
- `Events` — see above.
- `LevelGenerator` — see above.

## networking

TCP, length-prefixed CBOR frames (`Framing`, `Wire`). sealed `Envelope`:
`Join` -> `Welcome(slot, SessionConfig)` then `Input` up / `Snapshot` down.
client side, `NetworkGameSession.connect(host, port)` runs the handshake and
returns the `GameSession` (seat + config bound for its whole life). UDP
broadcast discovery on 54545. server re-stamps input slot — client cannot
hijack seat. CBOR decode ignores unknown keys: ADD fields with defaults freely,
never remove/retype without bumping `PROTOCOL_VERSION`.

## recipes

**new augment.** data object implementing `Augment` + capability trait in
`ecs/component/augment/`. systems honor traits via `augments.with<Trait>()`,
never name concrete augment. see `DoubleJump` + `GrantsAirJumps` + `JumpSystem`.

**new hazard / enemy.** spawn entity with `Physics` + `Transform` +
`RenderShape` + `Lethal` (+ own component & movement system slotted before
physics). contact -> `LethalContact` -> death, free. snapshot picks it up free
(`Transform`+`RenderShape` -> `PropSnap`). copy `BoulderSystem` as template.

**new biome / floor theme.** implement `LevelGenerator`, construct it in
`GameInstance` (today `ClassicGenerator` hardwired there — that line is the
swap point; route by `SessionConfig` when more than one exists).

**react to run moment (pickup, reward, run-end effect).** new system after the
emitting system, before `EventFlushSystem`. read `events.each<PlatformCleared>`
etc. mutate `RunConfig` / grant augment there.

**new wire data.** add field with default to `WorldSnapshot`/`Envelope` in
`shared`. old clients ignore it. test round-trip in `SnapshotWireTest`.

## invariants — break these, suffer

- NEVER mutate Box2D world inside contact callback. emit event, act next system.
- spawn/despawn bodies only between steps. cross-thread ops go through
  `GameInstance.pendingOps`.
- Fleks `Entity.hashCode` crashes on RoboVM (iOS). no Entity in Set/Map keys.
  list only (`PlatformC.touchedBy`).
- layout randomness only inside `LevelGenerator`, via `MathUtils.random`, or
  seeded runs break.
- feel scale: game tuned from YIKES at 0.2x realtime. new velocity x0.2, new
  accel x0.04 of YIKES numbers.
- `shared` / `client-shared` / `bot` import no gdx/fleks/ktx. `client` touches
  `server` only via `DedicatedServer`. arch tests enforce.
- `EventFlushSystem` last, always.

## tests

- `server/unit` — one system, hand-poked world.
- `server/component` — full headless run via `buildSim()` (`SimHarness`,
  `ScriptedClimber` bot). same systems as production minus render.
- `server/integration` + `e2e` — real sockets.
- `arch` — module + import rules.

`buildSim(seed = N)` = reproducible run. test new mechanics there first;
cheapest place to catch regressions.
