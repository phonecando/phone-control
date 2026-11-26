// Phone control: the snapshot reads the composed tools pick targets with,
// the act timeout, and one composed run end to end against a stub bridge.
import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';

process.env.PHONECANDO_BRIDGE_URL = 'http://bridge.test';
process.env.PHONECANDO_BRIDGE_TOKEN = 't';
const { controlTools } = await import('./control/index.ts');
const { actTimeoutMs } = await import('./control/pce.ts');
const { editableTarget, flatScreen, observationKey, textNode, visible } = await import('./control/snapshot.ts');

const saved = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = saved;
});

// A screen 1000x2000: a list holding an OK row whose label is not clickable
// itself, a field, and a node off screen.
const SNAPSHOT = {
  snapshotId: 7,
  package: 'com.example',
  activity: '.Main',
  focused: null,
  device: { display: [1000, 2000] },
  nodes: [
    { i: 0, parent: null, bounds: [0, 0, 1000, 2000] },
    { i: 1, parent: 0, scrollable: true, bounds: [0, 0, 1000, 1500] },
    { i: 2, parent: 1, clickable: true, bounds: [0, 100, 1000, 200] },
    { i: 3, parent: 2, text: 'OK', bounds: [10, 110, 200, 190] },
    { i: 4, parent: 0, editable: true, text: '', desc: 'Search', bounds: [0, 1600, 1000, 1700] },
    { i: 5, parent: 0, text: 'Hidden', bounds: [0, 2100, 1000, 2200] },
  ],
};

describe('snapshot reads', () => {
  it('see only nodes with their centre on the display', () => {
    assert.deepEqual(SNAPSHOT.nodes.filter((n) => visible(n, SNAPSHOT)).map((n) => n.i), [0, 1, 2, 3, 4]);
    assert.equal(textNode(SNAPSHOT, 'hidden'), null);
    assert.equal(textNode(SNAPSHOT, 'ok').i, 3);
    assert.equal(editableTarget(SNAPSHOT).i, 4);
  });

  it('flatten the screen to labelled elements, filtered on request', () => {
    const { elements } = flatScreen(SNAPSHOT, false, false);
    assert.deepEqual(elements.map((e) => e.text ?? e.desc), ['OK', 'Search']);
    assert.deepEqual(elements[0], { id: '', clickable: false, editable: false, scrollable: false, bounds: '10 110 200 190', centerX: 105, centerY: 150, text: 'OK' });
    assert.deepEqual(flatScreen(SNAPSHOT, true, false).elements.map((e) => e.text), ['OK']);
    assert.deepEqual(flatScreen(SNAPSHOT, false, true).elements.map((e) => e.desc), ['Search']);
  });

  it('key an observation by its event sequence and what it shows', () => {
    assert.notEqual(observationKey({ ...SNAPSHOT, eventSeq: 1 }), observationKey({ ...SNAPSHOT, eventSeq: 2 }));
    assert.equal(observationKey(SNAPSHOT), observationKey({ ...SNAPSHOT, snapshotId: 8 }));
  });
});

describe('actTimeoutMs', () => {
  it('covers the action, its expect and a margin, within the caps', () => {
    assert.equal(actTimeoutMs({}), 40_000);
    assert.equal(actTimeoutMs({ timeoutMs: 5 }), 11_000);
    assert.equal(actTimeoutMs({ timeoutMs: '60000', expect: {}, expectTimeoutMs: 2_000 }), 72_000);
    assert.equal(actTimeoutMs({ timeoutMs: 999_999, expect: {}, expectTimeoutMs: 999_999 }), 160_000);
  });
});

describe('phone_click', () => {
  it('observes, then taps the clickable parent of the matching text', async () => {
    const calls = [];
    globalThis.fetch = async (url, init) => {
      const body = JSON.parse(init.body);
      calls.push([String(url).replace('http://bridge.test', ''), body]);
      const data = calls.length === 1 ? SNAPSHOT : { done: true };
      return new Response(JSON.stringify({ ok: true, data }), { status: 200 });
    };
    const click = controlTools.find((t) => t.name === 'phone_click');
    // The result names the node it tapped: the row, which has no text.
    assert.equal(await click.run({ text: 'ok' }), '{"text":""}');
    assert.deepEqual(calls.map((c) => c[0]), ['/pce/observe', '/pce/act']);
    assert.deepEqual(calls[1][1], { snapshotId: 7, action: { type: 'tap', target: { type: 'node', nodeId: 2 } }, timeoutMs: 30_000 });
  });

  it('hands back the error text when nothing matches', async () => {
    globalThis.fetch = async () => new Response(JSON.stringify({ ok: true, data: SNAPSHOT }), { status: 200 });
    const click = controlTools.find((t) => t.name === 'phone_click');
    assert.equal(await click.run({ text: 'nope' }), 'error NOT_FOUND: Element not found');
  });
});
