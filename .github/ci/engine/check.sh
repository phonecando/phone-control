#!/usr/bin/env bash
# The engine's quick check, the same wherever it runs (this repo's
# engine.yml, a dev's machine, or the app repo that carries this one as its
# phone-control/ folder): the static dependency law (check-deps.mjs), the
# pure-JVM modules' tests, and the sample's output against
# engine/docs/guides/quickstart.md. engine.yml runs the rest: the whole
# build, every JDK and OS, each module alone, the published coordinates and
# the device harness.
#
# Usage: check.sh fast [gradle args...]   e.g. --offline
set -euo pipefail
export LC_ALL=C

[ "${1:-}" = fast ] || { echo "usage: check.sh fast [gradle args...]" >&2; exit 2; }
shift
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HERE/../../../engine"
out=build/ci
mkdir -p "$out"

node "$HERE/check-deps.mjs" .
./gradlew "$@" --console=plain :api:test :core:test :json:test :pack:test :testkit:test :gates:test :bridge:test
./gradlew "$@" -q :sample:run > "$out/sample.txt"
cat "$out/sample.txt"
node "$HERE/expect-output.mjs" --doc docs/guides/quickstart.md --after 'Expected sample output' "$out/sample.txt"
