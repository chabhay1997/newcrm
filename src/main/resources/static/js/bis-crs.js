document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.crs-alert.success').forEach(alert => {
    window.setTimeout(() => {
      alert.classList.add('is-dismissing');
      window.setTimeout(() => alert.remove(), 250);
    }, 4000);
  });

  const sizeForm = document.querySelector('.crs-size-form');
  const sizeSelect = document.getElementById('crs-size');
  if (sizeForm && sizeSelect) {
    sizeSelect.addEventListener('change', async () => {
      const url = new URL(sizeForm.action, window.location.origin);
      new FormData(sizeForm).forEach((value, key) => url.searchParams.set(key, String(value)));
      url.searchParams.set('page', '0');
      sizeSelect.disabled = true;
      try {
        const response = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
        if (!response.ok) throw new Error(`Request failed with status ${response.status}`);
        const nextDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
        const currentTable = document.querySelector('.crs-table-wrap');
        const currentFooter = document.querySelector('.crs-footer');
        const nextTable = nextDocument.querySelector('.crs-table-wrap');
        const nextFooter = nextDocument.querySelector('.crs-footer');
        if (!currentTable || !currentFooter || !nextTable || !nextFooter) throw new Error('Table response is incomplete');
        currentTable.replaceWith(nextTable);
        currentFooter.replaceWith(nextFooter);
        const currentCount = document.querySelector('.crs-title-wrap p');
        const nextCount = nextDocument.querySelector('.crs-title-wrap p');
        if (currentCount && nextCount) currentCount.textContent = nextCount.textContent;
        window.history.replaceState({}, '', url.pathname + url.search);
        window.initBisCrsPagination?.();
      } catch (error) {
        window.location.assign(url.pathname + url.search);
      } finally {
        sizeSelect.disabled = false;
      }
    });
  }

  const modal = document.getElementById('crs-create-modal');
  const open = document.getElementById('open-crs-create');
  const close = document.getElementById('close-crs-create');
  const cancel = document.getElementById('cancel-crs-create');
  if (!modal || !open) return;
  open.addEventListener('click', () => modal.showModal());
  [close, cancel].forEach(button => button?.addEventListener('click', () => modal.close()));
  modal.addEventListener('click', event => { if (event.target === modal) modal.close(); });
});
