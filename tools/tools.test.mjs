// Every phone tool keeps the shape agents and MCP servers rely on: a unique
// phone_* name, a description, a closed JSON Schema object and its channel.
import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { basicTools } from './basic.ts';
import { controlTools } from './control/index.ts';
import { forChannel } from './tool.ts';

const all = [...basicTools, ...controlTools];

describe('the phone tools', () => {
  it('have unique phone_* names', () => {
    const names = all.map((t) => t.name);
    assert.deepEqual(names.filter((n) => !/^phone_[a-z_]+$/.test(n)), []);
    assert.equal(new Set(names).size, names.length);
  });

  it('describe themselves and take a closed object of arguments', () => {
    for (const tool of all) {
      assert.ok(tool.description.length > 20, tool.name);
      assert.equal(tool.input.type, 'object', tool.name);
      assert.equal(tool.input.additionalProperties, false, tool.name);
      for (const key of tool.input.required ?? []) assert.ok(key in tool.input.properties, `${tool.name} requires ${key}`);
      assert.equal(typeof tool.run, 'function', tool.name);
    }
  });

  it('ship the basic ones everywhere and phone control on full only', () => {
    assert.deepEqual(basicTools.filter((t) => t.channel !== 'all').map((t) => t.name), []);
    assert.deepEqual(controlTools.filter((t) => t.channel !== 'full').map((t) => t.name), []);
  });

  it('are offered by channel', () => {
    assert.equal(forChannel(all, 'full').length, all.length);
    assert.deepEqual(forChannel(all, 'lite'), basicTools);
    assert.deepEqual(forChannel(all, 'play'), basicTools);
  });
});
