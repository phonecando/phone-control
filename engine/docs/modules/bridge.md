# bridge

`pce-bridge`, pure JVM; `org.json` is compile-only (Android's own at run
time). The phone routes as Request → Reply handlers, with no HTTP in it: the
host frames requests and calls `BridgeRouter.handle(caller, method, path, body)`.

- `BridgeRouter` — every gate in order (`../../../docs/safety-model.md`).
- `PhoneRoute` / `BridgeRoute` — the routes and their guards; the host adds
  its own routes, which pass every phone gate.
- `PceRoutes` (`/pce/*`), `AppAccessRoutes` (`/app-access`), `AppTargets`
  (the app a launch opens), `ActivityRoutes` (`/open` with its scheme
  allowlist, `/share`, `/app`), `ClipboardRoutes` (`/clipboard`),
  `HandsRoutes` (`/hands/over`, `/hands/wait`), `PromptRoutes` (`/unlock`,
  `/accessibility`).
- `CallerAuth` — the bearer token names the `Caller`, compared in constant time.
- `Reply` — the envelope and every error code (`../../../docs/bridge.md`);
  `A11yFixes` — what an agent is told per accessibility state.
- Ports: `Caller`, `AutomationHost`, `UserQuestions` (the user's answers to
  an unlock, the accessibility service, an app), `HandsDesk` (the host's
  hand-over store), `Activities` and `Clipboard` (Android's intents and
  clipboard), `AppReturn` (bringing the app back for `/app`).

`./gradlew :bridge:test`.
