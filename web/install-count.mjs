const counters = document.querySelectorAll('[data-install-count]');
const storageKey = 'fullLockRegisteredDevices';
const valid = value => Number.isSafeInteger(value) && value >= 0;

function display(count, cached = false) {
  for (const badge of counters) {
    badge.hidden = false;
    badge.querySelector('strong').textContent = new Intl.NumberFormat('th-TH').format(count);
    badge.querySelector('span:last-child').textContent = cached ? 'เครื่อง · ยอดล่าสุด' : 'เครื่อง';
  }
}

if (counters.length) {
  let saved;
  try { saved = JSON.parse(localStorage.getItem(storageKey)); } catch {}
  if (valid(saved?.count)) display(saved.count, true);
  if (!valid(saved?.count) || !Number.isFinite(saved.time) || Date.now() - saved.time > 900000 || saved.time > Date.now()) {
    fetch('/api/install-count', { signal: AbortSignal.timeout(6000) })
      .then(response => {
        if (!response.ok) throw new Error('Count unavailable');
        return response.json();
      })
      .then(({ registeredDevices }) => {
        if (!valid(registeredDevices)) throw new Error('Invalid count');
        display(registeredDevices);
        try {
          localStorage.setItem(storageKey, JSON.stringify({ count: registeredDevices, time: Date.now() }));
        } catch {}
      })
      .catch(() => {
        if (valid(saved?.count)) return;
        for (const badge of counters) {
          badge.hidden = false;
          badge.querySelector('span:first-child').textContent = 'ยอดใช้งาน';
          badge.querySelector('strong').textContent = '—';
          badge.querySelector('span:last-child').textContent = 'รออัปเดต';
        }
      });
  }
}
