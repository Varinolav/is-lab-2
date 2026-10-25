import React, { useEffect, useState } from 'react';
import MovieImport from './MovieImport.jsx';

const statuses = { SUCCESS: 'Успешно', FAILED: 'Ошибка' };
const pageSize = 20;

export default function ImportHistory({ request, revision, onChanged }) {
  const [operations, setOperations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [page, setPage] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    request('imports')
      .then((result) => {
        if (active) setOperations([...result].sort((a, b) => b.id - a.id));
      })
      .catch((cause) => { if (active) setError(cause.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [request, revision, refresh]);

  const pageCount = Math.max(1, Math.ceil(operations.length / pageSize));
  const currentPage = Math.min(page, pageCount - 1);
  const rows = operations.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

  return <>
    <MovieImport request={request} onFinished={(success) => {
      setPage(0);
      setRefresh((value) => value + 1);
      if (success) onChanged();
    }} />
    <section className="list-panel" aria-label="История импортов">
    <div className="list-toolbar">
      <div><h1 className="history-title">История импортов</h1>
        <span className="list-count">{loading ? 'Загрузка…' : `Операций: ${operations.length}`}</span>
      </div>
      <button className="button ghost" disabled={loading} onClick={() => setRefresh((value) => value + 1)}>Обновить</button>
    </div>
    {error && <div className="message error history-error" role="alert">{error}</div>}
    <div className="table-scroll">
      <table>
        <thead><tr><th scope="col">ID операции</th><th scope="col">Дата</th><th scope="col">Статус</th>
          <th scope="col">Пользователь</th><th scope="col">Добавлено фильмов</th></tr></thead>
        <tbody>{!loading && !error && rows.map((operation) => <tr key={operation.id}>
          <td>{operation.id}</td>
          <td>{operation.creationDate ? operation.creationDate.replace('T', ' ').slice(0, 19) : '—'}</td>
          <td><span className={`import-status ${operation.status === 'SUCCESS' ? 'success' : operation.status === 'FAILED' ? 'failed' : ''}`}>
            {statuses[operation.status] ?? operation.status}
          </span></td>
          <td>{operation.username}</td>
          <td>{operation.status === 'SUCCESS' ? operation.count ?? '—' : '—'}</td>
        </tr>)}</tbody>
      </table>
      {loading && <div className="empty-state" role="status">Загружаем историю…</div>}
      {!loading && !error && operations.length === 0 && <div className="empty-state">
        <strong>Импортов пока нет</strong><p>Здесь появятся результаты операций импорта.</p>
      </div>}
    </div>
    {!loading && !error && operations.length > 0 && <div className="pagination">
      <span>Страница {currentPage + 1} из {pageCount}</span>
      <div><button className="button secondary" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Назад</button>
        <button className="button secondary" disabled={currentPage + 1 >= pageCount} onClick={() => setPage(currentPage + 1)}>Вперёд</button></div>
    </div>}
    </section>
  </>;
}
