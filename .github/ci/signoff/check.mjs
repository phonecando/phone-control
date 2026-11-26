#!/usr/bin/env node
// Every commit of a pull request is signed off (git commit -s): LICENSE
// section 7 asks it of each contribution, as the contributor's record that
// they agree to that section. Bots' pull requests (Dependabot) are not
// contributions and pass at once. Zero deps.
//
//   BASE_SHA=<sha> HEAD_SHA=<sha> AUTHOR_TYPE=<User|Bot> \
//     node .github/ci/signoff/check.mjs
import { spawnSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseLog, unsigned } from './rules.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', '..');
const { BASE_SHA, HEAD_SHA, AUTHOR_TYPE } = process.env;

if (AUTHOR_TYPE === 'Bot') {
  console.log('A bot\'s pull request: nothing to sign off.');
  process.exit(0);
}
if (!/^[0-9a-f]{40}$/.test(BASE_SHA ?? '') || !/^[0-9a-f]{40}$/.test(HEAD_SHA ?? '')) {
  console.error('BASE_SHA and HEAD_SHA must be commit ids');
  process.exit(2);
}

const log = spawnSync('git', ['log', '--no-merges', '--format=%H%x1f%an%x1f%ae%x1f%B%x1e', `${BASE_SHA}..${HEAD_SHA}`], {
  cwd: ROOT,
  encoding: 'utf8',
});
if (log.status !== 0) {
  console.error(`git log ${BASE_SHA}..${HEAD_SHA} failed: ${log.stderr.trim()}`);
  process.exit(1);
}
const commits = parseLog(log.stdout);
const missing = unsigned(commits);
for (const commit of missing) {
  console.log(
    `::error::${commit.sha.slice(0, 12)} (${commit.name}) has no Signed-off-by line. Sign off every commit to ` +
      `agree to section 7 of LICENSE: git commit -s, or for commits already made, ` +
      `git rebase --signoff ${BASE_SHA.slice(0, 12)} and force-push.`,
  );
}
console.log(`${commits.length} commit(s), ${missing.length} not signed off.`);
process.exit(missing.length ? 1 : 0);
