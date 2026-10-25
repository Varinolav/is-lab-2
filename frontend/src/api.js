export async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  if (!(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
  headers.set('X-Requested-With', 'fetch');
  const response = await fetch(`/api/${path}`, { ...options, credentials: 'same-origin', headers });
  const raw = await response.text();
  let data = raw;
  if (raw) {
    try { data = JSON.parse(raw); } catch {}
  }
  if (!response.ok) {
    if (response.status === 401 && path !== 'auth/login' && path !== 'auth/register') {
      window.dispatchEvent(new Event('session-expired'));
    }
    if (response.status === 401) throw new Error(data?.error || 'Необходимо войти в систему.');
    if (response.status >= 500) throw new Error('Ошибка сервера. Не удалось выполнить запрос.');
    if (response.status === 404) throw new Error('Объект не найден. Обновите список.');
    if (response.status === 409 && (data?.error || data?.message)) throw new Error(data.error || data.message);
    if (response.status === 409 || /foreign key|constraint.*references|still referenced/i.test(raw)) {
      throw new Error('Этот объект используется другими записями. Сначала измените связанные записи.');
    }
    throw new Error(data?.error || data?.message || (typeof data === 'string' && data.trim()
      ? data.replace(/\[PARAMETER\]|\[\]/g, '').trim() : `Ошибка сервера (${response.status}).`));
  }
  return data;
}
