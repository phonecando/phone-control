# Modules

| Module | Artifact | Platform | Depends on | Purpose | Docs |
|---|---|---|---|---|---|
| `api` | `pce-api` | JVM | kotlinx.coroutines | Public contracts: snapshots, nodes, selectors, actions, errors, backend. | `../modules/api.md` |
| `core` | `pce-core` | JVM | `api` | Assembler, registry, resolver, verifier, trace, scheduler, PCE facade, capability router. | `../modules/core.md` |
| `json` | `pce-json` | JVM | `api` + kotlinx.serialization | Optional contract DTOs and JSON round-trip. | `../modules/json.md` |
| `pack` | `pce-pack` | JVM | `api` + `json` | Signed declarative packs and manifests: Ed25519, validation, compatibility, flow runtime. | `../modules/pack.md` |
| `testkit` | `pce-testkit` | JVM | `api` | Fake backend, fixtures and backend contract helpers. | `../modules/testkit.md` |
| `android` | `pce-android` | Android | `api` | Accessibility backend: observe, windows, actions, observation screenshots. | `../modules/android.md` |
| `ime` | `pce-ime` | Android | `api` | Tiny text/Enter/Backspace IME backend. | `../modules/ime.md` |
| `shizuku` | `pce-shizuku` | Android | `api` | **WIP, optional**: privileged key/gesture/launch backend. | `../modules/shizuku.md` |
| `gates` | `pce-gates` | JVM | — | The rules that permit or block an agent: phone-control lock, app access, hand-over, screen lease, screenshot retention. | `../modules/gates.md` |
| `bridge` | `pce-bridge` | JVM | `gates` + platform `org.json` | The phone routes and the router that runs every gate in order, transport-agnostic. | `../modules/bridge.md` |
| `keystore` | `pce-keystore` | Android | `gates` | Keystore-signed lock and app answers (only right after a fingerprint or PIN); what the phone says about its apps. | `../modules/keystore.md` |
| `service` | `pce-service` | Android | `api`, `core`, `json`, `android`, `gates`, `keystore` | The accessibility service's control part: the PCE adapter and its gated backends. | `../modules/service.md` |
| `sample` | not published | JVM | `core`, `testkit` | Runnable proof that a host can use the PCE. | `../modules/sample.md` |
| `consumer-sample` | not published | JVM | published `pce-api` / `pce-core` | Optional published-coordinate proof. | `../modules/consumer-sample.md` |

The HTTP server, the biometric prompt itself, the tool surface and app UI
stay outside. They are host adapters: the host serves `bridge`'s routes,
asks for the fingerprint, then calls the `keystore` stores to sign.

See `architecture.md` for the dependency graph and module rules.
