import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from './api.js';

test('JSON requests send session credentials and CSRF header', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/auth/login');
    assert.equal(options.credentials, 'same-origin');
    assert.equal(options.headers.get('X-Requested-With'), 'fetch');
    assert.equal(options.headers.get('Content-Type'), 'application/json');
    return new Response('{"username":"user","role":"USER"}', { status: 200 });
  };
  assert.deepEqual(await api('auth/login', { method: 'POST', body: '{}' }), { username: 'user', role: 'USER' });
});

test('multipart upload lets browser supply boundary', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  globalThis.fetch = async (url, options) => {
    assert.equal(options.headers.has('Content-Type'), false);
    assert.equal(options.headers.get('X-Requested-With'), 'fetch');
    return new Response('{"importedCount":1}', { status: 201 });
  };
  const body = new FormData();
  body.append('file', new Blob(['[]'], { type: 'application/json' }), 'movies.json');
  assert.deepEqual(await api('movies/import', { method: 'POST', body }), { importedCount: 1 });
});

test('registration sends selected role and preserves duplicate-login message', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/auth/register');
    assert.equal(JSON.parse(options.body).role, 'ADMIN');
    return new Response('{"error":"Пользователь с таким логином уже существует."}', { status: 409 });
  };
  await assert.rejects(api('auth/register', { method: 'POST',
    body: JSON.stringify({ username: 'alice', password: 'example123', role: 'ADMIN' }),
  }), /Пользователь с таким логином уже существует/);
});

test('401 drops authenticated UI, invalid login only shows its error', async (t) => {
  const originalFetch = globalThis.fetch;
  const originalWindow = globalThis.window;
  t.after(() => { globalThis.fetch = originalFetch; globalThis.window = originalWindow; });
  const events = [];
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
  globalThis.fetch = async () => new Response('{"error":"Неверный логин или пароль."}', { status: 401 });
  await assert.rejects(api('auth/login', { method: 'POST' }), /Неверный логин или пароль/);
  assert.deepEqual(events, []);
  await assert.rejects(api('movies'), /Неверный логин или пароль/);
  assert.deepEqual(events, ['session-expired']);
});
