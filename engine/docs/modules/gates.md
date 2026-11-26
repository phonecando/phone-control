# gates

`pce-gates`, pure JVM, no dependencies. The rules that decide whether an
agent's call may reach the screen. Every class is pure; clocks and stores
are injected.

- `controllock/` — `ControlLock` (install, launch or ask mode; who is
  unlocked), `AskGrants` (ask mode's per-agent unlocks, idle after
  `AskGrants.IDLE_MS`), `LockMode`, `LockStore` (port; `keystore` signs it).
- `appaccess/` — `AppAccess` (one list for every agent: Always, Allow all,
  No, and each agent's Just now), `AppAccessRules` (the verdict order),
  `AppGate` (one call's check of every look and action), `ForeignParts`
  (widgets and notification rows of other apps inside a window),
  `OwnQuestions` (no taps on the app's own questions), `JustNowGrants`,
  `AppAccessStore` (port).
- `hands/` — `HandsRules` (who has the controls after a hand-over),
  `HandsGate` (what a screen call gets meanwhile), `HandOver`, `HandWait`,
  `UserAct`, `HandsText` (what the agent is told).
- `lease/` — `ScreenLease` (one agent drives at a time; the line, the
  reservation, the idle time), `ScreenNotices` (port).
- `a11y/` — `A11yStatusRules` (the accessibility service's state from
  what Android and the service say: off, starting, not_started, failed,
  stopped, ready), `A11yWatch` (a start's failure, whether it ran, since
  when it is switched on but unbound), `A11yStatus`, `TestDrivers` (the
  test tools that hold Android's automation connection).
- `shots/` — `ShotBook`: when an agent's screenshot is deleted (its next
  one, another agent getting the screen, its agent done, five minutes).

Test fixtures (`testFixtures`): `FakeLockStore`, `unlockedLock`,
`FakeAppAccessStore`, `appAccess`. `./gradlew :gates:test`.
