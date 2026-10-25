import React, { useEffect, useState } from 'react';
import App from './App.jsx';
import { api } from './api.js';

export default function AuthGate() {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [registering, setRegistering] = useState(false);

  useEffect(() => {
    let active = true;
    const expired = () => { setUser(null); setError('Сессия завершена. Войдите снова.'); };
    window.addEventListener('session-expired', expired);
    api('auth/me').then((value) => { if (active) { setUser(value); setError(''); } })
      .catch((cause) => { if (active) setError(cause.message === 'Необходимо войти в систему.' ? '' : cause.message); })
      .finally(() => { if (active) setChecking(false); });
    return () => { active = false; window.removeEventListener('session-expired', expired); };
  }, []);

  async function submit(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      if (registering) {
        await api('auth/register', { method: 'POST', body: JSON.stringify({
          username: data.get('username'), password: data.get('password'), role: data.get('role'),
        }) });
        setRegistering(false);
      }
      setUser(await api('auth/login', { method: 'POST', body: JSON.stringify({
        username: data.get('username'), password: data.get('password'),
      }) }));
    } catch (cause) { setError(cause.message); }
    finally { setBusy(false); }
  }

  async function logout() {
    setBusy(true);
    setError('');
    try { await api('auth/logout', { method: 'POST' }); setUser(null); }
    catch (cause) { setError(cause.message); }
    finally { setBusy(false); }
  }

  if (checking) return <main className="login-shell"><p role="status">Проверяем сессию…</p></main>;
  if (user) return <>
    <div className="session-bar"><span>{user.username} · {user.role === 'ADMIN' ? 'Администратор' : 'Пользователь'}</span>
      <button className="button ghost" onClick={logout} disabled={busy}>Выйти</button>
      {error && <span role="alert">{error}</span>}
    </div>
    <App key={user.username} />
  </>;
  return <main className="login-shell"><section className="login-panel">
    <p className="subheading">Каталог фильмов</p><h1>{registering ? 'Регистрация' : 'Вход в систему'}</h1>
    <p>{registering ? 'Создайте учётную запись и выберите роль.' : 'Войдите под своей учётной записью.'}</p>
    {error && <div className="message error" role="alert">{error}</div>}
    <form key={registering ? 'register' : 'login'} onSubmit={submit}>
      <label className="field"><span>Логин</span><input name="username" autoComplete="username" required maxLength={100} /></label>
      <label className="field"><span>Пароль</span><input name="password" type="password" autoComplete={registering ? 'new-password' : 'current-password'} required maxLength={1024} /></label>
      {registering && <label className="field"><span>Роль</span><select name="role" defaultValue="USER">
        <option value="USER">Пользователь (USER)</option>
        <option value="ADMIN">Администратор (ADMIN)</option>
      </select></label>}
      <button className="button primary" disabled={busy}>{busy ? 'Подождите…' : registering ? 'Зарегистрироваться' : 'Войти'}</button>
    </form>
    <button className="text-button" disabled={busy} onClick={() => { setRegistering(!registering); setError(''); }}>
      {registering ? 'Уже есть аккаунт? Войти' : 'Создать аккаунт'}
    </button>
  </section></main>;
}
