# Phone tools

The `phone_*` tools an AI agent calls to use an Android phone: their names,
descriptions and argument schemas, and the code each runs. Every tool calls
the phone's loopback bridge over HTTP, so the agent never touches Android
itself. Standalone TypeScript with zero dependencies.

- `tool.ts`: what a tool is (`PhoneTool`), and `forChannel`, the tools a
  build may offer.
- `client.ts`: the bridge client. The URL and the engine's own token come
  from the env (`PHONECANDO_BRIDGE_URL`, `PHONECANDO_BRIDGE_TOKEN`); a
  result is always text for the model, the data as JSON or a readable error.
- `basic.ts`: the tools every build ships (ping, battery, notify, focus,
  share, open, clipboard).
- `control/`: phone control, full builds only: the typed PCE tools
  (`phone_observe`, `phone_act`, ...), the compatibility tools composed from
  observe + act (`composed.ts`, picking targets with `snapshot.ts`) and the
  prompts that ask the user (unlock, an app's first use, accessibility
  setup, handing over the controls). A build without phone control leaves
  this folder out entirely.

The names, descriptions and schemas are a contract: prompts and saved chats
rely on them, so keep them stable. An agent's adapter registers the tools
with its own plugin API; a tool that makes one bridge call can also be
served over MCP from a table of routes.

Relative imports name their `.ts` file, so Node (22.18+) runs the code as
it is. Tests: `node --test "*.test.mjs"` in this folder (`npm test`).
