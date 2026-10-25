import React, { useRef, useState } from 'react';

export default function MovieImport({ request, onFinished }) {
  const [file, setFile] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const dragDepth = useRef(0);

  function selectFiles(files) {
    if (busy) return;
    setError('');
    setNotice('');
    if (files.length === 0) return;
    if (files.length !== 1 || !files[0].name.toLowerCase().endsWith('.json')) {
      setFile(null);
      setError('Выберите один файл с расширением .json.');
      return;
    }
    setFile(files[0]);
  }

  async function submit(event) {
    event.preventDefault();
    if (!file || busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    const data = new FormData();
    data.append('file', new Blob([file], { type: 'application/json' }), file.name);
    let success = false;
    try {
      const result = await request('movies/import', { method: 'POST', body: data });
      success = true;
      setNotice(`Импорт завершён. Добавлено фильмов: ${result.importedCount}.`);
      setFile(null);
    } catch (cause) {
      setError(cause instanceof TypeError ? 'Не удалось связаться с сервером.' : cause.message);
    } finally {
      setBusy(false);
      onFinished(success);
    }
  }

  return <section className="list-panel movie-import" aria-label="Импорт фильмов">
    <h2 className="history-title">Импорт фильмов</h2>
    <form onSubmit={submit} aria-busy={busy}>
      <div className={`import-dropzone ${dragging ? 'dragging' : ''} ${busy ? 'busy' : ''}`}
        onDragEnter={(event) => {
          event.preventDefault();
          if (!busy && event.dataTransfer.types.includes('Files')) {
            dragDepth.current++;
            setDragging(true);
          }
        }}
        onDragOver={(event) => { event.preventDefault(); event.dataTransfer.dropEffect = busy ? 'none' : 'copy'; }}
        onDragLeave={(event) => {
          event.preventDefault();
          dragDepth.current = Math.max(0, dragDepth.current - 1);
          if (dragDepth.current === 0) setDragging(false);
        }}
        onDrop={(event) => {
          event.preventDefault();
          event.stopPropagation();
          dragDepth.current = 0;
          setDragging(false);
          selectFiles(event.dataTransfer.files);
        }}>
        <strong>{dragging ? 'Отпустите файл здесь' : 'Перетащите JSON-файл сюда'}</strong>
        <span>или выберите файл на компьютере</span>
        <label className="button secondary import-file-picker">
          Выбрать файл
          <input className="visually-hidden" type="file" accept=".json,application/json" disabled={busy}
            aria-label="JSON-файл для импорта" onChange={(event) => {
              selectFiles(event.target.files);
              event.target.value = '';
            }} />
        </label>
      </div>
      {file && <div className="import-selected" role="status">
        <span><strong>{file.name}</strong> · {(file.size / 1024).toFixed(1)} КБ</span>
        <button type="button" className="text-button" disabled={busy}
          onClick={() => { setFile(null); setError(''); setNotice(''); }}>Убрать файл</button>
      </div>}
      <div className="import-actions"><button className="button primary" disabled={!file || busy}>
        {busy ? 'Импортируем…' : 'Импортировать'}
      </button></div>
      {error && <div className="message error" role="alert">{error}</div>}
      {notice && <div className="message success" role="status">{notice}</div>}
    </form>
  </section>;
}
