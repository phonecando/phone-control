# Bridge contract

The phone routes an agent's tools call (`tools/`), as the app serves them
on `http://127.0.0.1:<port>`. The routes and their guards are
`engine/bridge` (`PhoneRoute`, `BridgeRouter`); the app adds routes of its
own that are not phone control, and those pass every phone gate.

## Auth

Every request carries `Authorization: Bearer <token>`, the token the
agent's engine got at launch. A missing or unknown token is
`401 unauthorized` and nothing else runs. The token names the agent; there
is no other identity.

## Envelope

```json
{"ok": true, "data": { ... }}
{"ok": false, "code": "...", "message": "..."}
```

The HTTP status is for the bridge's own refusals. A call that ran and did
not work (a PCE error, no app to share to) is `200` with `ok: false`. POST
bodies are UTF-8 JSON objects; anything else is `400 bad_json`.

## Routes

| Route | Guards | What it does |
|---|---|---|
| `GET /pce/describe` | full, gated, pce | Backend, capabilities, packs. |
| `POST /pce/observe` | full, gated, pce, screen | A snapshot of the screen. |
| `POST /pce/act` | full, gated, pce, screen | One action on a snapshot, with an optional `expect`. |
| `POST /pce/pack` | full, gated, pce, screen | List, refresh, roll back or run a signed pack. |
| `POST /open` | gated, screen | `{url}`: open a link; `http`, `https`, `mailto`, `tel`, `geo`, `market` only (else `400 bad_scheme`). `{started}`. |
| `POST /share` | gated, screen | `{text, title?}`: the share sheet. `{started}`. |
| `POST /clipboard` | gated | `{action: "set", text}` or `{action: "get"}`. `{text, available}` on get. |
| `POST /app` | screen | `{tab?, notify?}`: bring the app forward; hands the screen back (`phone_return`). `{started, notified}`. |
| `POST /unlock` | full | `{waitMs?}` 5–90 s: ask the user to unlock phone control. `{requested, unlocked, notified}`. |
| `POST /accessibility` | full | `{waitMs?}` 30–280 s: ask the user to turn on the accessibility service. `{opened, enabled, cancelled, notified, restricted, state, cause?, fix?}`. |
| `POST /app-access` | full, gated | `{package, waitMs?}`: ask the user about one app; waits for the answer. |
| `POST /hands/over` | full, gated, screen | `{why, private?}`: hand the user the controls (`why` one line, at most 120 characters). |
| `POST /hands/wait` | full, gated | `{timeoutSec?}`: wait for the user to hand them back. `{back, mode?, message?}`. |

- **full**: only in a build with phone control; otherwise `404 not_found`.
- **gated**: needs the phone-control lock open for this agent.
- **pce**: needs the accessibility service connected.
- **screen**: takes the screen lease; refused while the user has the controls.

Checks run in this order; the first refusal answers: route, build, lock,
service, hands, body, app, lease. See `safety-model.md` for each one.

Why some routes need no lock:

- `/app` only brings the app itself forward, so the agent can hand the
  phone back (`phone_return`); it can't reach any other app or the screen
  of one. It is still a screen route, so it waits its turn in the line and
  never jumps over another agent's driving, and the holder's successful
  `/app` gives the screen back at once and ends its ask-mode unlock and
  its "Just now" apps.
- `/unlock`, `/accessibility` only ask the user; the user answers in the
  app, after their fingerprint or in Android's settings. They matter
  while the lock is shut or the service is off, so they can't need either.

`/hands/over` in WATCH blocks until the user hands the phone back or 240
seconds pass; private (PAUSED) answers at once and the agent ends its turn.
A wait that runs out while the user still has the phone says to call
`/hands/wait` again.

## Errors

| Status | Code | When | Extra fields |
|---|---|---|---|
| 400 | `bad_json` | Body is not a JSON object. | |
| 400 | `bad_request` | A field is missing or wrong. | |
| 401 | `unauthorized` | No or unknown bearer token. | |
| 403 | `automation_off` | Phone control is locked for this agent. | |
| 403 | `app_not_allowed` | The app is not on the list yet; call `phone_ask_app` with `package`. | `package`, `label` |
| 403 | `app_blocked` | The user said No to agents in this app. | `package`, `label` |
| 403 | `notification_hidden` | A tap on a notification whose app nobody can name. | `label` |
| 404 | `not_found` | No such route, or not in this build. | |
| 409 | `control_busy` | Another agent has the screen; you wait in line. | `holder`, `retryAfterMs`, `place` |
| 409 | `screen_refused` | A turn another agent's message started can't take the screen. | |
| 409 | `user_has_controls` | The user has the controls. | |
| 409 | `paused_by_user` | The user stopped you; end your turn. | |
| 409 | `user_deciding` | A question waits in the app; only the user answers it. | |
| 500 | `internal` | The bridge failed. | |
| 503 | `automation_unavailable` | The accessibility service isn't up; the message says the fix for its state. | `reason`, `cause` |
| 200 | PCE codes | The call ran and failed: `NOT_FOUND`, `AMBIGUOUS`, `STALE_SNAPSHOT`, `TIMEOUT`, `CANCELLED`, … | `recover`, `candidates` |

`/app-access` answers `{asked, allowed}`, plus `answer` (`"always"`,
`"once"`, `"no"` or null) and `notified` when the user was asked, and
`blocked: true` when the app ended up on the No list. It waits 30 to 240
seconds (`waitMs`, default 120000).

## The accessibility service's state

When the host can tell, `503 automation_unavailable` adds `reason`, the
service's state, and `cause` when one is known; `/accessibility` answers
with `state`, `cause` and, unless it is `ready`, `fix`, the same text the
503's message carries for that state (`A11yFixes`):

| `reason` / `state` | `cause` | Fix |
|---|---|---|
| `off` | `auto_off`, `restricted` or none | `phone_open_accessibility`: the user switches it on (allowing restricted settings first). |
| `starting` | | Try again in a few seconds. |
| `not_started` | `test_driver` or none | A test tool holds Android's automation connection: the user stops it or restarts the phone. Settings won't help. |
| `stopped` | `test_driver` or none | Same as `not_started`, after it had run. |
| `failed` | | The user switches it off and on, and reports it if it keeps failing; the message names what threw. |
| `absent` | | No service in this build. |

A plain `off` keeps the message it always had. `reason` and `cause` are
additive: the code and the other fields are unchanged.

## The screen lease

One agent drives at a time. The first screen call takes the screen; its
own calls always pass, so observe then act can't be split. It keeps the
screen until it is idle for 30 seconds, calls `/app`, or its run ends.
Others get `409 control_busy` with their place in line and are told when
the screen is theirs; it is kept for them for the same 30 seconds.

## What an observe returns

`/pce/observe` returns the snapshot (`snapshotId`, `package`, `activity`,
`windows`, `nodes`, `device`, `eventSeq`), and when the app gate left
something out, `hidden: [{package?, label, what}]`; a screenshot asked for
and not taken adds `screenshotWithheld` with why.
