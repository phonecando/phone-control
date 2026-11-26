# PhoneCanDo PCE

A modular Android phone-control engine (PCE) used by PhoneCanDo, and the
gates that decide what an agent may do with it (`gates`, `bridge`,
`keystore`, `service`). The whole engine works as one build; each module
can be consumed independently. PhoneCanDo owns the roadmap and is the
primary consumer.

Status: `0.1.0-dev` is the local development version. The PhoneCanDo app builds the
PCE from source through a Gradle composite build. Publishing to Maven local
is optional and is used by `consumer-sample/` and external consumers. Open
PCE work is tracked in `docs/planning/todo.md`.

## Start here

- `docs/README.md` — full documentation map.
- `docs/overview/architecture.md` — product boundary, mental model and
  dependency law.
- `docs/overview/modules.md` — module map and module docs.
- `docs/guides/quickstart.md` — build, test and optional publish.
- `docs/api/README.md` — consumer API guide.
- `docs/planning/todo.md` — current open work.
- `docs/release/known-limits.md` — known limits.

## Rules

`../AGENTS.md` (the repo root's) is the quality gate for module boundaries,
public API, safety, snapshots, actions, testing and licensing. Read it
before changing anything here.

## Docs

All engine documentation lives under `docs/`; `README.md` is the only
Markdown file at the engine root. The safety model and the bridge contract
are in the repo root's `docs/`.
