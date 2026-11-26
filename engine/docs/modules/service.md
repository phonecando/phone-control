# service

`pce-service`, Android library. The accessibility service's control part.

- `PceAccessibilityService` — the base service: the app in front, the app
  windows on screen, the last activity, the event count, the adapter's
  lifecycle. The host subclasses it (Android binds the subclass by name)
  and adds its parts through `ServiceHooks`.
- `PceAutomationImpl` — the `/pce/*` JSON adapter over one `Pce`.
- `GuardedBackend` — keeps the host's overlay (`OverlayGuard`) out of
  every look and touch; a touch never reported back times out.
- `appgate/AppGatedBackend` — judges every look and action of a call by its
  `AppGate`: leaves out other apps' parts, refuses taps on them, takes a
  screenshot only when everything on screen may be shown.
- Ports: `ServiceHooks` (its `failed` hears a start that threw, so the host
  can say so), `OverlayGuard`, `Packs`.
