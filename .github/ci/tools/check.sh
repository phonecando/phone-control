#!/usr/bin/env bash
# The phone_* tools' tests (tools/, zero dependencies: its package.json's
# npm test), the same wherever it runs: this repo's engine.yml or the app
# repo that carries this one as its phone-control/ folder. Nothing there
# yet: says so and passes.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../../tools" 2> /dev/null || { echo "no tools/ folder yet"; exit 0; }
shopt -s nullglob
tests=(*.test.mjs)
[ "${#tests[@]}" -gt 0 ] || { echo "tools/: no tests yet"; exit 0; }
node --test "${tests[@]}"
