# AGENTS.md — phone control (coding agents read this first)

This repo is PhoneCanDo's phone control: everything on the path from an
agent's call to a touch on the screen, and every rule that permits or
blocks it. It is published so anyone can see what an agent can and can't
do on a phone. The PhoneCanDo app mounts it as a submodule at
`phone-control/` and builds it from source; nothing here may depend on the
app. Every rule below is a quality gate, not a style preference.

## Layout

- `engine/` — one Gradle build (`rootProject.name = "phonecando-pce"`,
  coordinates `com.phonecando.pce:pce-<module>`):
  - the phone-control engine (PCE): `api`, `core`, `json`, `pack`,
    `testkit`, the backends `android`, `ime`, `shizuku`, and `sample`
    (`consumer-sample` is a separate optional build);
  - the gates: `gates` (the lock, app access, hand-over, screen lease),
    `bridge` (the phone routes and the router that runs every gate in
    order), `keystore` (answers signed only after a fingerprint),
    `service` (the accessibility service's control part).
  Docs in `engine/docs/` (`engine/docs/README.md` is the map), work in
  `engine/docs/planning/todo.md`.
- `tools/` — the `phone_*` tool definitions agents call (TypeScript), which
  reach the bridge's routes.
- `docs/` — `safety-model.md` (each gate, what it blocks, where it sits) and
  `bridge.md` (the phone routes' contract).

## 1. Product boundary

This repo owns:

- observation and snapshots, selectors and resolution, typed actions and
  execution, postconditions and verification, trace and packs (the PCE);
- who may drive and where: the phone-control lock, the app list, the
  hand-over, the screen lease, the order the bridge checks them in, and
  the signed records behind them;
- the phone routes' contract and the `phone_*` tool definitions.

The host app owns:

- the HTTP server, the biometric prompt, prompts and UI;
- app lifecycle, foreground service and notifications;
- its own routes that are not phone control, and anything it draws over
  the screen (it reaches us through `OverlayGuard` and `ServiceHooks`).

If a change needs an app type, an Expo/React Native type, NanoHTTPD, or a
private app path, it does not belong here. Where the gates need the host,
add a small port (an interface or a function) the host implements; never
reach into the app.

## 2. Dependency law

```
api <- core
api <- android
api <- ime
api <- shizuku
api <- json
api <- pack
json <- pack
api <- testkit
core + testkit <- sample
published api/core <- consumer-sample (optional)
gates <- bridge
gates <- keystore
api + core + json + android + gates + keystore <- service
```

- `api`, `core`, `json`, `pack`, `testkit`, `gates`, `bridge` are pure JVM.
  No `android.*`, no app packages, no platform imports. Only `bridge` uses
  `org.json`, compile-only (Android's own at run time; the real one in
  tests).
- `android`, `ime`, `shizuku` are backends and depend on `api` only. A
  backend must not pull in `core`; the host composes backend + core.
- `keystore` and `service` are the only host-side Android libraries.
  `service` composes the a11y backend with `core` and the gates.
- `gates` never depends on the PCE: an app build with no accessibility
  service still runs the lock, the app list and the router.
- No reverse edges and no cycles. `core` must never import a backend.
- Use `api` for types that are part of a module's public surface; use
  `implementation` for internals. A consumer must not need an accidental
  transitive dependency.
- Each module must build, test and publish alone. Add a module to
  `settings.gradle.kts` only when it has real code and a green build; no
  empty modules.

## 3. Public API law

- Public API is versioned; use `0.1.0-dev` while pre-1.0. Semver and
  migration notes apply when a real external consumer exists.
- Mark unstable PCE API with `@ExperimentalPceApi` before 1.0.
- Every public type and non-obvious function has KDoc or a comment.
- No global mutable state or singletons. One `Pce` instance owns one
  backend, one current snapshot and one id sequence; the host makes the
  process's one lock and app list.
- Inject `Clock` and `IdGenerator` (or a clock function); never call a wall
  clock or random source directly from PCE or gate logic.
- Expected failures return `PceResult.Err` with an `ErrorCode`. Do not throw
  for "not found", "ambiguous", "stale", "timeout" or backend unavailability.
- Backends are the only platform boundary. The core speaks `Backend`,
  `RawSnapshot` and `BackendAction`; it never sees Android classes.

## 4. Safety law

- A gate only ever gets stricter by mistake: a record that does not check
  out, a missing key, an unknown package or a broken store must refuse or
  ask, never allow.
- Anything that lets an agent in further (unlock, a mode change, Always,
  Allow all, taking a No off under Allow all) needs a record signed right
  after the user's fingerprint or PIN (`keystore`). Anything that only
  blocks more needs nothing.
- No agent call reaches the lock's unlock or mode, the app list's Always or
  Allow all, or the user's answer to a question.
- Keep stored formats, prefs keys, Keystore aliases, route paths, JSON
  shapes and error codes stable: the app and agents depend on them.
  `docs/bridge.md` is the contract; change it in the same change.
- A new gate, or a gate that moves, updates `docs/safety-model.md`.

## 5. Snapshot and action law

- `SnapshotId` is stable for one PCE instance and never reused.
- `NodeId` is dense per snapshot: `nodes[i].id == NodeId(i)`.
- Actions always carry the snapshot id they were resolved against. A stale
  id is rejected; never silently act on a changed screen.
- Resolution is deterministic: multiple matches return `AMBIGUOUS` with
  candidate ids. Do not silently pick the first or "best" node. `index` is
  the explicit disambiguator.
- Backends own their `BackendNodeId`; the core translates a resolved node
  back to that id before executing.
- Declare `Capability` honestly. The PCE rejects an action before calling a
  backend that does not declare the capability.

## 6. Module rules

- `api`: contracts only. No implementation, no Android, no I/O.
- `core`: assembler, registry, resolver, verifier, PCE, scheduler, trace.
- `json`: optional contract serialization. Never force serialization on
  consumers of `api`/`core`.
- `pack`: signed declarative pack loading, validation and flow runtime;
  depends on `api` + `json`; never executes pack code.
- `testkit`: fakes and fixtures. Must not depend on `core`; it implements
  the public `Backend` contract.
- `android`, `ime`, `shizuku`: one platform concern each. Keep Android
  lifecycle code in the adapter; keep selectors and decisions in `core`.
- `gates`: pure rules, stores and clocks injected; fakes in its
  `testFixtures`.
- `bridge`: Request → Reply, no HTTP; the host frames requests.
- `keystore`: the signed stores and what the phone says about its apps.
- `service`: the accessibility service's control part; the host's parts
  come in through `ServiceHooks`.
- `sample`: runnable proof that a host can use the PCE without PhoneCanDo.
- `consumer-sample`: optional published-coordinate proof; it is not part of
  the composite `settings.gradle.kts` and, when run, must not use project
  dependencies.

## 7. Testing

- Every non-trivial branch has a test. Pure logic is tested in pure JVM
  tests (the gates and the bridge use JUnit 4, the PCE JUnit 5).
- Backend contract tests run against each backend via `testkit`'s
  `BackendContract.verify`; Android-specific behavior also needs
  instrumentation/device evidence.
- `cd engine && ./gradlew build` must be green before a slice is marked
  done. Run `:sample:run` for PCE changes.
- `tools/`: `cd tools && npm test` (Node 22.18+, no dependencies).
- No sizing/layout/device claim without logs or pixels.
- Keep fixtures in `testkit` (PCE) or `gates`' test fixtures reusable; do
  not copy them into every test.

## 8. Reuse and licensing

- Prefer famous, multi-maintainer, permissive dependencies for edges only:
  AndroidX, kotlinx.coroutines, Shizuku, optional MCP Kotlin SDK, host-side
  test tools.
- Build the PCE core, the gates and the a11y backend clean-room. Do not copy
  code from projects without a verified license. Do not embed AGPL
  projects.
- Record a new dependency's license and why it is not replaceable in
  `engine/docs/planning/plan.md` or `engine/docs/planning/todo.md`.
- Host-side tools (uiautomator2, Maestro, Appium, mobile-mcp) are test
  oracles, never runtime dependencies.

## 9. Workflow

- One concern per commit-sized change. Update
  `engine/docs/planning/plan.md` and `engine/docs/planning/todo.md` when
  scope or status changes.
- Run the narrowest test first, then `./gradlew build` for the module set
  you touched.
- Never run git commands. We own git; use file tools and report.
- Do not add a feature, option or abstraction nobody asked for. The
  smallest complete change wins.

## Review checklist

- [ ] Does this keep the pure modules free of Android and app types?
- [ ] Is state instance-owned and are clocks and stores injected?
- [ ] Are expected failures typed `PceResult.Err`?
- [ ] Do actions carry a snapshot id, and is staleness rejected?
- [ ] Are multiple matches explicit `AMBIGUOUS`, not silently ranked?
- [ ] Does every gate still fail closed, and is `docs/safety-model.md` true?
- [ ] Are route paths, JSON shapes and error codes unchanged, or is
      `docs/bridge.md` updated with them?
- [ ] Does every touched module still build and test alone?
- [ ] Did `engine/docs/planning/plan.md` / `todo.md` get updated?
