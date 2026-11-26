# Phone control

PhoneCanDo runs AI coding agents on your Android phone, and lets them use
the phone itself: look at the screen, tap, type, open apps. This repo is
that part, published so anyone can see exactly what an agent can and can't
do on a phone, and check that the app runs this code.

Everything on the path from an agent's call to a touch on the screen is
here, and so is every rule that permits or blocks it:

- you unlock phone control, right after your fingerprint, and pick how long
  it lasts;
- you say which apps agents may use; a No holds over everything;
- you can take the controls back at any time;
- only one agent drives at a time;
- an agent never sees other apps' widgets or notifications it may not use,
  and never answers the app's own questions.

`docs/safety-model.md` walks the path gate by gate; `docs/bridge.md` is the
routes' contract.

## What is in it

- `engine/` — one Gradle build:
  - `api`, `core`, `json`, `pack`, `testkit` — the phone-control engine
    (PCE): snapshots, selectors, typed actions, verification, signed packs;
  - `android`, `ime`, `shizuku` — backends (accessibility service, a tiny
    keyboard, an optional privileged one);
  - `gates` — the phone-control lock, the app list, the hand-over, the
    screen lease, how long screenshots stay;
  - `bridge` — every phone route (look, act, packs, open, share,
    clipboard, hand-over, unlock and app questions) and the router that
    runs every gate in order;
  - `keystore` — the lock and app answers, signed with Keystore keys that
    sign only right after your fingerprint or PIN;
  - `service` — the accessibility service's control part.
- `tools/` — the `phone_*` tools agents call.
- `docs/` — the safety model and the bridge contract.

The PhoneCanDo app (private) adds its server, its UI and anything it draws
on screen, through small ports these modules define. None of them can skip
a gate.

## Build and test

```sh
cd engine && ./gradlew build     # every module, JVM tests, Android lint
cd tools && npm test             # the phone_* tools (Node 22.18+)
```

JDK 17 and the Android SDK (compile SDK 36) build the engine. More in
`engine/README.md` and `engine/docs/guides/quickstart.md`.

## The app, its releases and its issues

This repo is the code only. The PhoneCanDo app itself, closed source, is
at [phonecando.app](https://phonecando.app), and its releases live in
[phonecando/distribution](https://github.com/phonecando/distribution): its
workflow builds and signs each APK from a commit on this repo's `main`, and
attests the APKs and that commit (`phonecando-<version>-phone-control.txt`),
so you can trace a release back to this code
(`gh attestation verify <apk> --repo phonecando/distribution`).

Found a bug in the app, want an agent added, or have an idea? Tell us in
[phonecando/feedback](https://github.com/phonecando/feedback). Issues here
are for this code.

## Licence and security

Source-available: see `LICENSE`. Third-party code: `THIRD_PARTY_NOTICES.md`.
Found a hole? Please read `SECURITY.md` and report it privately.

Working on the code? Read `AGENTS.md` first.
