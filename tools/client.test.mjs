// The bridge client: one call per tool run, its result always text for the
// model. client.ts reads the bridge's URL and token when it loads, so each
// setup imports a fresh copy.
import assert from 'node:assert/strict';
import { afterEach, describe, it } from 'node:test';

const URL_ = 'http://bridge.test';
const saved = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = saved;
});

let copies = 0;
async function client(env) {
  process.env.PHONECANDO_BRIDGE_URL = env.url ?? '';
  process.env.PHONECANDO_BRIDGE_TOKEN = env.token ?? '';
  return import(`./client.ts?copy=${(copies += 1)}`);
}

// A bridge that answers every call with `status` and `body`, and records the calls.
function bridge(status, body) {
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push({ url, method: init.method, headers: init.headers, body: init.body });
    return new Response(typeof body === 'string' ? body : JSON.stringify(body), { status });
  };
  return calls;
}

describe('call', () => {
  it('GETs without a body and POSTs JSON with one, with the engine token', async () => {
    const { call } = await client({ url: URL_, token: 't0k' });
    const calls = bridge(200, { ok: true, data: { percent: 80 } });
    assert.equal(await call('/battery'), '{"percent":80}');
    assert.equal(await call('/notify', { text: 'hi' }), '{"percent":80}');
    assert.deepEqual(
      calls.map((c) => [c.url, c.method, c.headers.authorization, c.body]),
      [
        [`${URL_}/battery`, 'GET', 'Bearer t0k', undefined],
        [`${URL_}/notify`, 'POST', 'Bearer t0k', '{"text":"hi"}'],
      ],
    );
  });

  it('turns errors into readable text', async () => {
    const { call } = await client({ url: URL_, token: 't' });
    bridge(403, { ok: false, code: 'automation_off', message: 'Phone control is locked' });
    assert.equal(await call('/pce/observe', {}), 'error automation_off: Phone control is locked');
    bridge(502, '<html>bad gateway</html>');
    assert.equal(await call('/ping'), 'bridge 502: <html>bad gateway</html>');
    globalThis.fetch = async () => {
      throw new Error('connection refused');
    };
    assert.equal(await call('/ping'), 'bridge call failed: connection refused');
  });

  it('gives up after its timeout', async () => {
    const { call } = await client({ url: URL_, token: 't' });
    globalThis.fetch = (_url, init) =>
      new Promise((_resolve, reject) => init.signal.addEventListener('abort', () => reject(new Error('aborted'))));
    assert.equal(await call('/ping', undefined, undefined, 20), 'bridge timeout (0s)');
  });

  it('says so when no bridge started the engine', async () => {
    const { call } = await client({});
    bridge(200, { ok: true });
    assert.match(await call('/ping'), /^phonecando bridge unavailable/);
  });
});

describe('errorText', () => {
  it('names the screen holder and the place in line on control_busy', async () => {
    const { errorText } = await client({ url: URL_, token: 't' });
    assert.match(errorText({ code: 'control_busy', holder: 'Builder', place: 2 }, 409), /^error control_busy: Builder holds the screen; you are #2 in line\./);
    assert.match(errorText({ code: 'control_busy' }, 409), /^error control_busy: another agent holds the screen\./);
    assert.equal(errorText({}, 500), 'error 500: unknown');
  });
});
