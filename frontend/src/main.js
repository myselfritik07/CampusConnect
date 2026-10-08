import './styles.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const STORAGE_KEY = 'campusconnect-events';
const form = document.querySelector('#event-form');
const list = document.querySelector('#event-list');
const status = document.querySelector('#connection-status');
const message = document.querySelector('#message');
const formError = document.querySelector('#form-error');
const submitButton = document.querySelector('#submit-button');
let events = [];
let localMode = false;

function escapeHtml(value = '') {
  return String(value).replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);
}

function readLocalEvents() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    return Array.isArray(saved) ? saved : [];
  } catch {
    return [];
  }
}

function saveLocalEvents() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(events));
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    const error = new Error(problem.message || `Request failed (${response.status}).`);
    error.status = response.status;
    throw error;
  }
  return response.status === 204 ? null : response.json();
}

function renderEvents() {
  document.querySelector('#event-count').textContent = String(events.length);
  if (!events.length) {
    list.innerHTML = '<p class="empty-state">No events yet. Add the first one using the form.</p>';
    return;
  }

  list.innerHTML = events.map((event) => {
    const date = new Date(event.startTime);
    const dateLabel = Number.isNaN(date.getTime())
      ? event.startTime
      : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date);
    return `<article class="event-row">
      <div class="event-info">
        <h3>${escapeHtml(event.title)}</h3>
        <p>${escapeHtml(event.category)} · ${escapeHtml(event.venue)} · ${escapeHtml(dateLabel)}</p>
      </div>
      <button class="remove-button" type="button" data-id="${Number(event.id)}" aria-label="Remove ${escapeHtml(event.title)}">Remove</button>
    </article>`;
  }).join('');
}

function setLocalMode() {
  localMode = true;
  status.textContent = 'Saved in this browser';
  message.textContent = 'The API is unavailable. Events are being saved on this device.';
}

async function loadEvents() {
  try {
    events = await apiRequest('/api/events');
    if (!Array.isArray(events)) throw new Error('The API returned an invalid event list.');
    status.textContent = 'Connected to event service';
    message.textContent = '';
  } catch {
    setLocalMode();
    events = readLocalEvents();
  }
  renderEvents();
}

function buildEvent() {
  return {
    title: form.elements.title.value.trim(),
    category: form.elements.category.value,
    venue: form.elements.venue.value.trim(),
    startTime: form.elements.startTime.value,
  };
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  formError.textContent = '';
  const newEvent = buildEvent();
  if (new Date(newEvent.startTime) <= new Date()) {
    formError.textContent = 'Choose a date and time in the future.';
    return;
  }

  submitButton.disabled = true;
  try {
    if (!localMode) {
      try {
        await apiRequest('/api/events', { method: 'POST', body: JSON.stringify(newEvent) });
      } catch (error) {
        if (error.status && error.status < 500) throw error;
        setLocalMode();
        events = readLocalEvents();
        events.push({ ...newEvent, id: Date.now() });
        saveLocalEvents();
      }
    } else {
      events.push({ ...newEvent, id: Date.now() });
      saveLocalEvents();
    }
    form.reset();
    if (!localMode) message.textContent = 'Event added.';
    await loadEvents();
  } catch (error) {
    formError.textContent = error.message;
  } finally {
    submitButton.disabled = false;
  }
});

list.addEventListener('click', async (event) => {
  const button = event.target.closest('[data-id]');
  if (!button) return;
  const id = Number(button.dataset.id);
  try {
    if (localMode) {
      events = events.filter((item) => Number(item.id) !== id);
      saveLocalEvents();
    } else {
      try {
        await apiRequest(`/api/events/${id}`, { method: 'DELETE' });
      } catch (error) {
        if (error.status && error.status < 500) throw error;
        setLocalMode();
        events = events.filter((item) => Number(item.id) !== id);
        saveLocalEvents();
      }
    }
    message.textContent = 'Event removed.';
    await loadEvents();
  } catch (error) {
    message.textContent = error.message;
  }
});

loadEvents();