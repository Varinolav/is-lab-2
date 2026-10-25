import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { selectTableRows } from './tableData.js';
import Operations from './Operations.jsx';
import { api } from './api.js';

const sections = [
  { key: 'movies', title: 'Фильмы', singular: 'фильм', endpoint: 'movies' },
  { key: 'persons', title: 'Люди', singular: 'человека', endpoint: 'persons' },
  { key: 'coordinates', title: 'Координаты', singular: 'координаты', endpoint: 'coordinates' },
  { key: 'locations', title: 'Локации', singular: 'локацию', endpoint: 'locations' },
];

const colors = ['RED', 'BLUE', 'YELLOW', 'WHITE', 'BROWN'];
const countries = ['RUSSIA', 'SPAIN', 'CHINA', 'INDIA'];
const ratings = ['G', 'PG_13', 'R', 'NC_17'];
const genres = ['DRAMA', 'ADVENTURE', 'TRAGEDY', 'THRILLER'];

const empty = (value) => value === null || value === undefined || value === '';
const display = (value) => empty(value) ? '—' : String(value);
const personLabel = (person) => person ? `${person.name} · #${person.id}` : '—';
const coordinateLabel = (coordinates) => coordinates ? `${coordinates.x}; ${coordinates.y} · #${coordinates.id}` : '—';
const locationLabel = (location) => location ? `${location.name || 'Без названия'} · #${location.id}` : '—';
const dateLabel = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '—';

const columns = {
  movies: [
    ['id', 'ID', (item) => item.id],
    ['name', 'Название', (item) => item.name],
    ['coordinates', 'Координаты', (item) => coordinateLabel(item.coordinates)],
    ['creationDate', 'Создан', (item) => dateLabel(item.creationDate)],
    ['oscarsCount', 'Оскары', (item) => item.oscarsCount],
    ['budget', 'Бюджет', (item) => display(item.budget)],
    ['totalBoxOffice', 'Сборы', (item) => display(item.totalBoxOffice)],
    ['mpaaRating', 'Рейтинг', (item) => item.mpaaRating],
    ['director', 'Режиссёр', (item) => personLabel(item.director)],
    ['screenwriter', 'Сценарист', (item) => personLabel(item.screenwriter)],
    ['operator', 'Оператор', (item) => personLabel(item.operator)],
    ['length', 'Длительность', (item) => display(item.length)],
    ['goldenPalmCount', 'Золотые пальмы', (item) => display(item.goldenPalmCount)],
    ['genre', 'Жанр', (item) => display(item.genre)],
  ],
  persons: [
    ['id', 'ID', (item) => item.id],
    ['name', 'Имя', (item) => item.name],
    ['eyeColor', 'Цвет глаз', (item) => display(item.eyeColor)],
    ['hairColor', 'Цвет волос', (item) => display(item.hairColor)],
    ['location', 'Локация', (item) => locationLabel(item.location)],
    ['height', 'Рост', (item) => display(item.height)],
    ['nationality', 'Страна', (item) => display(item.nationality)],
  ],
  coordinates: [
    ['id', 'ID', (item) => item.id],
    ['x', 'X', (item) => item.x],
    ['y', 'Y', (item) => item.y],
  ],
  locations: [
    ['id', 'ID', (item) => item.id],
    ['x', 'X', (item) => item.x],
    ['y', 'Y', (item) => item.y],
    ['z', 'Z', (item) => item.z],
    ['name', 'Название', (item) => display(item.name)],
  ],
};

const textColumns = {
  movies: [
    { key: 'name', label: 'Название', value: (item) => item.name },
    { key: 'creationDate', label: 'Создан', value: (item) => dateLabel(item.creationDate) },
    { key: 'mpaaRating', label: 'Рейтинг', value: (item) => item.mpaaRating },
    { key: 'director', label: 'Режиссёр', value: (item) => personLabel(item.director) },
    { key: 'screenwriter', label: 'Сценарист', value: (item) => personLabel(item.screenwriter) },
    { key: 'operator', label: 'Оператор', value: (item) => personLabel(item.operator) },
    { key: 'genre', label: 'Жанр', value: (item) => display(item.genre) },
  ],
  persons: [
    { key: 'name', label: 'Имя', value: (item) => item.name },
    { key: 'eyeColor', label: 'Цвет глаз', value: (item) => display(item.eyeColor) },
    { key: 'hairColor', label: 'Цвет волос', value: (item) => display(item.hairColor) },
    { key: 'location', label: 'Локация', value: (item) => locationLabel(item.location) },
    { key: 'nationality', label: 'Страна', value: (item) => display(item.nationality) },
  ],
  coordinates: [],
  locations: [{ key: 'name', label: 'Название', value: (item) => display(item.name) }],
};

function initialForm(kind, item = {}) {
  if (kind === 'movies') return {
    name: item.name ?? '', coordinatesId: String(item.coordinates?.id ?? ''),
    oscarsCount: String(item.oscarsCount ?? 0), budget: String(item.budget ?? ''),
    totalBoxOffice: String(item.totalBoxOffice ?? ''), mpaaRating: item.mpaaRating ?? 'G',
    directorId: String(item.director?.id ?? ''), screenwriterId: String(item.screenwriter?.id ?? ''),
    operatorId: String(item.operator?.id ?? ''), length: String(item.length ?? ''),
    goldenPalmCount: String(item.goldenPalmCount ?? ''), genre: item.genre ?? '',
  };
  if (kind === 'persons') return {
    name: item.name ?? '', eyeColor: item.eyeColor ?? '', hairColor: item.hairColor ?? '',
    locationId: String(item.location?.id ?? ''), height: String(item.height ?? ''),
    nationality: item.nationality ?? '',
  };
  if (kind === 'coordinates') return { x: String(item.x ?? ''), y: String(item.y ?? '') };
  return { x: String(item.x ?? ''), y: String(item.y ?? ''), z: String(item.z ?? ''), name: item.name ?? '' };
}

function optionalNumber(value) {
  return value === '' ? null : Number(value);
}

function buildPayload(kind, form, references) {
  const byId = (list, id) => list.find((item) => String(item.id) === id) ?? null;
  if (kind === 'movies') return {
    name: form.name.trim(), coordinates: byId(references.coordinates, form.coordinatesId),
    oscarsCount: Number(form.oscarsCount), budget: optionalNumber(form.budget),
    totalBoxOffice: optionalNumber(form.totalBoxOffice), mpaaRating: form.mpaaRating,
    director: byId(references.persons, form.directorId),
    screenwriter: byId(references.persons, form.screenwriterId),
    operator: byId(references.persons, form.operatorId),
    length: optionalNumber(form.length), goldenPalmCount: optionalNumber(form.goldenPalmCount),
    genre: form.genre || null,
  };
  if (kind === 'persons') return {
    name: form.name.trim(), eyeColor: form.eyeColor || null, hairColor: form.hairColor || null,
    location: byId(references.locations, form.locationId),
    height: optionalNumber(form.height), nationality: form.nationality || null,
  };
  if (kind === 'coordinates') return { x: Number(form.x), y: Number(form.y) };
  return { x: Number(form.x), y: Number(form.y), z: Number(form.z), name: form.name.trim() || null };
}

function Field({ label, children, hint }) {
  return <label className="field"><span>{label}</span>{children}{hint && <small>{hint}</small>}</label>;
}

function SelectField({ label, value, onChange, options, placeholder = 'Не выбрано', required = false }) {
  return <Field label={label}>
    <select value={value} onChange={(event) => onChange(event.target.value)} required={required}>
      <option value="">{placeholder}</option>
      {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
    </select>
  </Field>;
}

function enumOptions(values) {
  return values.map((value) => ({ value, label: value }));
}

function referenceOptions(items, label) {
  return items.map((item) => ({ value: String(item.id), label: label(item) }));
}

function Editor({ kind, item, references, onClose, onSaved }) {
  const [form, setForm] = useState(() => initialForm(kind, item ?? {}));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const set = (key) => (value) => setForm((current) => ({ ...current, [key]: value }));
  const input = (key, type = 'text', extra = {}) => ({
    type, value: form[key], onChange: (event) => set(key)(event.target.value), ...extra,
  });

  async function submit(event) {
    event.preventDefault();
    if (kind === 'coordinates' && Number(form.x) <= -59) {
      setError('Координата X должна быть больше -59.');
      return;
    }
    if (kind === 'persons' && form.height !== '' && Number(form.height) <= 0) {
      setError('Рост должен быть больше 0.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const endpoint = sections.find((section) => section.key === kind).endpoint;
      await api(item ? `${endpoint}/${item.id}` : endpoint, {
        method: item ? 'PUT' : 'POST',
        body: JSON.stringify(buildPayload(kind, form, references)),
      });
      onSaved();
    } catch (cause) {
      setError(cause.message);
    } finally {
      setSaving(false);
    }
  }

  const needs = kind === 'movies'
    ? (!references.coordinates.length || !references.persons.length)
    : kind === 'persons' && !references.locations.length;

  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="modal editor" role="dialog" aria-modal="true" aria-labelledby="editor-title">
      <div className="modal-header">
        <div><p className="subheading">{item ? `Запись #${item.id}` : 'Новая запись'}</p>
          <h2 id="editor-title">{item ? 'Изменить' : 'Добавить'} {sections.find((section) => section.key === kind).singular}</h2></div>
        <button type="button" className="icon-button" aria-label="Закрыть" onClick={onClose}>×</button>
      </div>
      <form onSubmit={submit}>
        {needs && <div className="form-note">Сначала добавьте {kind === 'movies' ? 'координаты и хотя бы одного человека' : 'локацию'} в соответствующих разделах.</div>}
        {kind === 'movies' && <>
          <div className="form-grid">
            <Field label="Название *"><input {...input('name', 'text', { required: true, maxLength: 255 })} /></Field>
            <SelectField label="Рейтинг MPAA *" value={form.mpaaRating} onChange={set('mpaaRating')} options={enumOptions(ratings)} required />
            <SelectField label="Жанр" value={form.genre} onChange={set('genre')} options={enumOptions(genres)} />
            <SelectField label="Координаты *" value={form.coordinatesId} onChange={set('coordinatesId')}
              options={referenceOptions(references.coordinates, coordinateLabel)} required />
            <Field label="Оскары *"><input {...input('oscarsCount', 'number', { min: 0, step: 1, required: true })} /></Field>
            <Field label="Золотые пальмы"><input {...input('goldenPalmCount', 'number', { min: 1, step: 1 })} /></Field>
            <Field label="Бюджет"><input {...input('budget', 'number', { min: 1, step: 1 })} /></Field>
            <Field label="Сборы"><input {...input('totalBoxOffice', 'number', { min: 1, step: 1 })} /></Field>
            <Field label="Длительность"><input {...input('length', 'number', { min: 1, step: 1 })} /></Field>
          </div>
          <h3 className="form-group-title">Съёмочная группа</h3>
          <div className="form-grid">
            <SelectField label="Режиссёр *" value={form.directorId} onChange={set('directorId')}
              options={referenceOptions(references.persons, personLabel)} required />
            <SelectField label="Оператор *" value={form.operatorId} onChange={set('operatorId')}
              options={referenceOptions(references.persons, personLabel)} required />
            <SelectField label="Сценарист" value={form.screenwriterId} onChange={set('screenwriterId')}
              options={referenceOptions(references.persons, personLabel)} />
          </div>
        </>}
        {kind === 'persons' && <div className="form-grid">
          <Field label="Имя *"><input {...input('name', 'text', { required: true, maxLength: 255 })} /></Field>
          <SelectField label="Локация *" value={form.locationId} onChange={set('locationId')}
            options={referenceOptions(references.locations, locationLabel)} required />
          <SelectField label="Цвет глаз" value={form.eyeColor} onChange={set('eyeColor')} options={enumOptions(colors)} />
          <SelectField label="Цвет волос" value={form.hairColor} onChange={set('hairColor')} options={enumOptions(colors)} />
          <Field label="Рост"><input {...input('height', 'number', { step: 'any' })} /></Field>
          <SelectField label="Страна" value={form.nationality} onChange={set('nationality')} options={enumOptions(countries)} />
        </div>}
        {kind === 'coordinates' && <div className="form-grid">
          <Field label="X *" hint="Больше -59"><input {...input('x', 'number', { step: 'any', required: true })} /></Field>
          <Field label="Y *"><input {...input('y', 'number', { step: 'any', required: true })} /></Field>
        </div>}
        {kind === 'locations' && <div className="form-grid">
          <Field label="X *"><input {...input('x', 'number', { step: 1, required: true })} /></Field>
          <Field label="Y *"><input {...input('y', 'number', { step: 'any', required: true })} /></Field>
          <Field label="Z *"><input {...input('z', 'number', { step: 'any', required: true })} /></Field>
          <Field label="Название"><input {...input('name', 'text', { maxLength: 255 })} /></Field>
        </div>}
        {error && <div className="message error" role="alert">{error}</div>}
        <div className="modal-actions">
          <button type="button" className="button secondary" onClick={onClose}>Отмена</button>
          <button className="button primary" disabled={saving || needs}>{saving ? 'Сохраняем…' : 'Сохранить'}</button>
        </div>
      </form>
    </section>
  </div>;
}

function Details({ kind, item, onClose, onEdit }) {
  return <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
    <section className="modal details" role="dialog" aria-modal="true" aria-labelledby="details-title">
      <div className="modal-header">
        <div><p className="subheading">Запись #{item.id}</p><h2 id="details-title">{item.name || `${sections.find((section) => section.key === kind).title} #${item.id}`}</h2></div>
        <button type="button" className="icon-button" aria-label="Закрыть" onClick={onClose}>×</button>
      </div>
      <dl className="details-grid">
        {columns[kind].map(([key, label, value]) => <div key={key}><dt>{label}</dt><dd>{value(item)}</dd></div>)}
      </dl>
      <div className="modal-actions"><button className="button primary" onClick={onEdit}>Изменить</button></div>
    </section>
  </div>;
}

export default function App() {
  const [active, setActive] = useState('movies');
  const [items, setItems] = useState([]);
  const [references, setReferences] = useState({ coordinates: [], persons: [], locations: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [modal, setModal] = useState(null);
  const [searchText, setSearchText] = useState('');
  const [filterColumn, setFilterColumn] = useState('name');
  const [filterValue, setFilterValue] = useState('');
  const [sortColumn, setSortColumn] = useState('name');
  const [sort, setSort] = useState('asc');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [revision, setRevision] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    if (active === 'operations') {
      setItems([]);
      setLoading(false);
      return;
    }
    try {
      const result = await api(active);
      setItems(Array.isArray(result) ? result : []);
    } catch (cause) {
      setError(cause.message);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [active, revision]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    const source = new EventSource('/api/changes');
    const refresh = () => setRevision((value) => value + 1);
    source.addEventListener('changed', refresh);
    source.addEventListener('open', refresh);
    return () => {
      source.removeEventListener('changed', refresh);
      source.removeEventListener('open', refresh);
      source.close();
    };
  }, []);

  const visible = useMemo(() => selectTableRows(items, textColumns[active] ?? [], {
    filterColumn, filterValue, sortColumn, sort, page, pageSize,
  }), [items, active, filterColumn, filterValue, sortColumn, sort, page, pageSize]);

  useEffect(() => { setPage((current) => Math.min(current, visible.pageCount - 1)); }, [visible.pageCount]);

  function changeSection(section) {
    setActive(section);
    setError('');
    setNotice('');
    setPage(0);
    setSearchText('');
    setFilterValue('');
    setFilterColumn(textColumns[section]?.[0]?.key ?? '');
    setSortColumn(textColumns[section]?.[0]?.key ?? '');
  }

  async function openEditor(item = null) {
    setError('');
    try {
      const [coordinates, persons, locations] = await Promise.all([
        api('coordinates'), api('persons'), api('locations'),
      ]);
      setReferences({ coordinates, persons, locations });
      setModal({ mode: 'edit', kind: active, item });
    } catch (cause) {
      setError(`Не удалось загрузить связанные записи: ${cause.message}`);
    }
  }

  async function remove(item) {
    if (!window.confirm(`Удалить запись #${item.id}?`)) return;
    setError('');
    try {
      await api(`${active}/${item.id}`, { method: 'DELETE' });
      setNotice('Запись удалена.');
      setRevision((value) => value + 1);
    } catch (cause) {
      setError(cause.message);
    }
  }

  const section = sections.find((entry) => entry.key === active);
  return <div className="app-shell">
    <header className="tabs-header">
      <nav aria-label="Разделы">
        {sections.map((entry) => <button key={entry.key} className={`nav-item ${active === entry.key ? 'active' : ''}`}
          onClick={() => changeSection(entry.key)} aria-current={active === entry.key ? 'page' : undefined}>
          {entry.title}
        </button>)}
        <button className={`nav-item ${active === 'operations' ? 'active' : ''}`}
          onClick={() => changeSection('operations')} aria-current={active === 'operations' ? 'page' : undefined}>
          Операции
        </button>
      </nav>
    </header>

    <main className="main-content">
      {active === 'operations' ? <Operations request={api} onChanged={() => setRevision((value) => value + 1)} revision={revision} /> : <>
      <div className="page-actions">
        <button className="button primary add-button" onClick={() => openEditor()}>+ Добавить {section.singular}</button>
      </div>

      {notice && <div className="message success" role="status">{notice}<button aria-label="Закрыть" onClick={() => setNotice('')}>×</button></div>}
      {error && <div className="message error" role="alert">{error}<button aria-label="Закрыть" onClick={() => setError('')}>×</button></div>}

      <section className="list-panel" aria-label={`Список: ${section.title}`}>
        <div className="list-toolbar">
          <span className="list-count">{loading ? 'Загрузка…' : `${visible.rows.length} из ${visible.total}`}</span>
          <button className="button ghost" onClick={() => setRevision((value) => value + 1)} disabled={loading}>Обновить</button>
        </div>
        <div className="filters">
          {textColumns[active].length > 0 && <>
          <label className="filter-select">Фильтр по
            <select value={filterColumn} onChange={(event) => { setFilterColumn(event.target.value); setSearchText(''); setFilterValue(''); setPage(0); }}>
              {textColumns[active].map((column) => <option key={column.key} value={column.key}>{column.label}</option>)}
            </select>
          </label>
          <form className="search-form" onSubmit={(event) => { event.preventDefault(); setPage(0); setFilterValue(searchText.trim()); }}>
            <label htmlFor="filter-search">Значение (точное совпадение)</label>
            <div><input id="filter-search" value={searchText} onChange={(event) => setSearchText(event.target.value)} />
              <button className="button secondary">Найти</button></div>
          </form>
          <label className="filter-select">Сортировать по
            <select value={sortColumn} onChange={(event) => { setSortColumn(event.target.value); setPage(0); }}>
              {textColumns[active].map((column) => <option key={column.key} value={column.key}>{column.label}</option>)}
            </select>
          </label>
          <label className="filter-select">Порядок
            <select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}>
              <option value="asc">А–Я</option><option value="desc">Я–А</option>
            </select>
          </label>
          </>}
          <label className="filter-select">На странице
            <select value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(0); }}>
              <option value={10}>10</option><option value={20}>20</option><option value={50}>50</option>
            </select>
          </label>
          {filterValue && <button className="text-button" onClick={() => { setSearchText(''); setFilterValue(''); setPage(0); }}>Сбросить</button>}
        </div>

        <div className="table-scroll">
          <table><thead><tr>{columns[active].map(([key, label]) => <th key={key}>{label}</th>)}<th className="actions-column">Действия</th></tr></thead>
            <tbody>{!loading && visible.rows.map((item) => <tr key={item.id}>
              {columns[active].map(([key, , value], index) => <td key={key}>
                {index === 1 ? <button className="row-link" onClick={() => setModal({ mode: 'details', kind: active, item })}>{display(value(item))}</button> : display(value(item))}
              </td>)}
              <td className="row-actions"><button onClick={() => openEditor(item)}>Изменить</button><button className="danger-link" onClick={() => remove(item)}>Удалить</button></td>
            </tr>)}</tbody>
          </table>
          {!loading && visible.total === 0 && <div className="empty-state"><strong>Записей пока нет</strong><p>{filterValue ? 'Проверьте точное значение или сбросьте фильтр.' : `Добавьте ${section.singular}, чтобы начать.`}</p></div>}
          {loading && <div className="empty-state">Загружаем записи…</div>}
        </div>
        <div className="pagination">
          <span>Страница {page + 1} из {visible.pageCount}</span>
          <div><button className="button secondary" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>Назад</button>
            <button className="button secondary" disabled={page + 1 >= visible.pageCount || loading} onClick={() => setPage(page + 1)}>Вперёд</button></div>
        </div>
      </section>
      </>}
    </main>

    {modal?.mode === 'edit' && <Editor key={`${modal.kind}-${modal.item?.id ?? 'new'}`} kind={modal.kind}
      item={modal.item} references={references} onClose={() => setModal(null)}
      onSaved={() => { setModal(null); setNotice('Запись сохранена.'); setRevision((value) => value + 1); }} />}
    {modal?.mode === 'details' && <Details kind={modal.kind} item={modal.item} onClose={() => setModal(null)}
      onEdit={() => { const item = modal.item; setModal(null); openEditor(item); }} />}
  </div>;
}
