# Safety model

What stands between an AI agent and your phone's screen, in the order a call
meets it. Every gate below is code in this repo; where it lives is in
brackets (`engine/<module>/…`).

## The path

```
agent calls a phone_* tool (tools/)
  → HTTP on 127.0.0.1, bearer token            (the app's server)
  → who is calling                             CallerAuth
  → BridgeRouter.handle, in this order:
      1. route exists in this build            PhoneRoute.fullOnly
      2. phone-control lock                    ControlLock
      3. accessibility service connected       AutomationHost.connected
      4. you have the controls                 HandsGate
      5. JSON body
      6. the app in front / launched / linked  AppAccess, OwnQuestions
      7. one driver at a time                  ScreenLease
      8. the handler; inside the PCE, every
         look and action judged again          AppGate via AppGatedBackend
      9. the result's app checked once more    AppAccess
  → accessibility backend → a touch on the screen
```

A call refused at any step never reaches the next one, and never takes the
screen. Error codes and their bodies are in `bridge.md`.

## 1. Who is calling — `bridge/CallerAuth`

Each agent's engine gets its own bridge token at launch; the bearer names
the agent. A missing or unknown token is 401 and nothing else runs, the
body is not even read. Tokens are compared in constant time and the loop
never stops early. The server listens on loopback only (the app's part).

## 2. The phone-control lock — `gates/controllock`

**Blocks:** every gated route (all `/pce/*`, `/open`, `/share`,
`/clipboard`, `/app-access`, `/hands/*`) with 403 `automation_off` until
you unlock.

Three modes (`LockMode`): **install** (unlock once, kept across restarts),
**launch** (unlock again after the app restarts or you close it), **ask**
(each agent asks for its own unlock, which ends when it hands the phone
back, its engine stops, or it goes 5 minutes without a phone-control call:
`AskGrants`). Only the app's own UI unlocks or changes the mode, each right
after your fingerprint or PIN; no agent route reaches them. In ask mode an
agent's `/unlock` shows you a question; you answer it, never the agent.

The saved mode and unlock are signed with Android Keystore keys that sign
only within 15 seconds of your fingerprint or screen-lock PIN
(`keystore/KeystoreLockStore`, `AuthBoundSigner`). Agents run as the app's
Linux user and can write its files, so an unsigned "unlocked" would let one
unlock itself. A record that does not check out reads as locked, and a mode
that does not check out reads as the strictest (ask).

## 3. The accessibility service — `bridge/AutomationHost`, `gates/a11y`

**Blocks:** the PCE routes with 503 `automation_unavailable` while the
accessibility service isn't up. Only you can turn it on, in Android's
settings. Builds without one (lite, play) have no PCE routes at all (404).

The refusal's `reason` says why (`A11yStatusRules`), and its message the
fix for that state (`bridge/A11yFixes`), so an agent never sends you to
Settings when Settings can't help:

| State | What it means | How we tell |
|---|---|---|
| `off` | Not switched on (cause `auto_off` or `restricted` when known) | Not in Android's enabled services, or Accessibility off |
| `starting` | Switched on, Android starting it | Bound but not up, or unbound for under 5 s |
| `not_started` | Switched on, Android won't start it | Not bound after 5 s; it never ran since switched on |
| `stopped` | It ran, then Android stopped it, switch still on | Not bound after 5 s; it ran since switched on |
| `failed` | Started, but setting up threw | The service's own report (`ServiceHooks.failed`) |
| `ready` | Up | The PCE adapter is up |

Android binds no accessibility service while a test tool (Maestro, UI
Automator, Appium, an adb instrumentation) holds its UiAutomation
connection, and says nothing: the switch stays on. So `not_started` and
`stopped` name the test tools installed (`TestDrivers`, cause
`test_driver`), and the fix is stopping the tool or restarting the phone.

## 4. You have the controls — `gates/hands`

When you take the phone over (or an agent hands it to you with
`phone_hand_over`), `HandsRules` decides who has it and `HandsGate` turns
away screen calls:

- the agent you took it from, while it may **watch**: only `/pce/observe`
  passes, anything else is 409 `user_has_controls`;
- the agent, **paused**: every screen call is 409 `paused_by_user`, and it
  is told to end its turn;
- any other agent: 409 `user_has_controls`.

Routes that leave the screen alone still pass. Nothing here stops an
agent's run; it stops its use of the screen.

## 5. Which apps — `gates/appaccess`

**Blocks:** a screen call in an app you have not let agents use: 403
`app_not_allowed` (the agent may ask you with `phone_ask_app`) or
`app_blocked` (you said No).

One list for every agent (`AppAccessRules`, in this order): the app itself
and the home screen are never asked about; **No** holds over everything,
even Allow all; then **Allow all**, **Always** and the agent's own **Just
now** (until it hands the phone back or stops). Always and Allow all are
signed right after your fingerprint (`keystore/KeystoreAppAccessStore`);
Allow all's record also signs the No list, so a No removed behind our back
turns Allow all off. No and taking an app off need nothing: they only ask
more.

Which app a call is judged by: a launch by the app it opens
(`bridge/AppTargets`), `/open` by the app Android hands the link to
(`keystore/PhoneApps`), anything else by the app in front. `/open` also
starts only `http`, `https`, `mailto`, `tel`, `geo` and `market` links
(`bridge/ActivityRoutes`): no `intent:`, `file:` or `content:` link
reaches Android.

`OwnQuestions`: while you have a question to answer in the app (an app, an
unlock, an agent's permission), an agent's tap or pack run on the app's own
screen is 409 `user_deciding`. Only you answer.

## 6. One driver at a time — `gates/lease/ScreenLease`

**Blocks:** a second agent's screen call while another has the screen: 409
`control_busy` with its place in line. The holder keeps the screen until it
has been idle for 30 seconds (counted from the end of its last call), hands
it back (`/app`), or its run ends; the next in line gets it reserved. A
turn another agent's message started never takes a free screen: 409
`screen_refused`. Your unlock or app question stops the asking agent's time
so nobody drives over the dialog.

## 7. Inside the call — `service/appgate`, `gates/appaccess/AppGate`

The router judged the app in front when the call came in, but a call goes
on: a tap's `expect`, a wait, every step of a pack. So every look and action
is judged again (`AppGatedBackend`, carrying the call's `AppGate`):

- a look is refused if its app is not allowed; parts of other apps inside
  the window (a widget, a notification row: `ForeignParts`) are left out
  unless the agent may use their app, and listed in `hidden`;
- a tap, long press or swipe that starts on a part left out is refused; a
  notification whose app nobody can name is 403 `notification_hidden`;
- a launch is judged by the app it opens, anything else by the app in front;
- a screenshot is taken only when nothing was left out, every app window on
  screen is allowed and it is not the home screen (whose widgets and Recents
  cards show other apps); it is judged again on what it shows, and one taken
  in a race is deleted at once.

The first app refused ends the call; the router answers with that app's
refusal, and an observe or pack result that names an app agents may not use
is swapped for the refusal too.

## 8. Screenshots don't linger — `gates/shots/ShotBook`

Every agent runs as the app's one Android user and can read the folder
screenshots land in. So a screenshot lives only while its agent needs it:
its next one replaces it, and it is deleted when another agent gets the
screen, when its agent hands the phone back or stops, and after five
minutes. The app deletes the files when the book says.

## 9. Out of the way — `service/GuardedBackend`

Anything the app draws over the screen is hidden for screenshots, lets
touches through while an agent's touch runs, and never shows in a
snapshot's window list. A touch Android never reports back gives up after
its own length plus 5 seconds, so a request can't hold the screen forever.

## What is not here

The app's server, its UI and the biometric prompt itself are the app's.
They reach these gates only through small ports (`Caller`,
`AutomationHost`, `UserQuestions`, `HandsDesk`, `Activities`, `AppReturn`,
`Clipboard`, `ServiceHooks`, `OverlayGuard`, `Packs`, the stores); none of
them can skip a gate. Asking the user (`UserQuestions`) only ever waits for
their answer: the route never answers for them.
