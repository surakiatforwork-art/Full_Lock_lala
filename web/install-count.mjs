const counters = document.querySelectorAll('[data-install-count]');

if (counters.length) {
  fetch('/api/install-count', { signal: AbortSignal.timeout(6000) })
    .then(response => {
      if (!response.ok) throw new Error('Count unavailable');
      return response.json();
    })
    .then(({ registeredDevices }) => {
      if (!Number.isSafeInteger(registeredDevices) || registeredDevices < 0) return;
      const count = new Intl.NumberFormat('th-TH').format(registeredDevices);
      for (const badge of counters) {
        badge.querySelector('strong').textContent = count;
        badge.hidden = false;
      }
    })
    .catch(() => {});
}
