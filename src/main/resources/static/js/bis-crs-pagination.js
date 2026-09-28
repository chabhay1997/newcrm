function initBisCrsPagination() {
  const pager = document.querySelector('.crs-footer.classic-pagination');
  if (!pager) return;

  const current = Number(pager.dataset.current);
  const totalPages = Number(pager.dataset.pages);
  const total = Number(pager.dataset.total);
  const size = Number(pager.dataset.size);
  const first = total ? (current - 1) * size + 1 : 0;
  const last = total ? Math.min(current * size, total) : 0;
  const urlFor = page => {
    const url = new URL(location.href);
    url.searchParams.set('page', String(page - 1));
    return url.pathname + url.search;
  };
  const pages = [];
  for (let page = Math.max(1, current - 2); page <= Math.min(totalPages, current + 2); page++) pages.push(page);

  const count = document.createElement('span');
  count.textContent = `Showing ${first} to ${last} of ${total} entries`;
  const controls = document.createElement('nav');
  controls.className = 'classic-pagination-controls';
  controls.setAttribute('aria-label', 'BIS CRS pages');

  const input = document.createElement('input');
  input.type = 'number';
  input.min = '1';
  input.max = String(totalPages);
  input.placeholder = 'Go to...';
  input.setAttribute('aria-label', 'Go to page');
  const go = () => {
    const page = Number(input.value);
    if (Number.isInteger(page) && page >= 1 && page <= totalPages) location.href = urlFor(page);
    else input.setCustomValidity(`Enter a page from 1 to ${totalPages}.`);
  };
  input.addEventListener('input', () => input.setCustomValidity(''));
  input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); go(); } });

  const goButton = document.createElement('button');
  goButton.type = 'button';
  goButton.className = 'go-button';
  goButton.textContent = 'Go';
  goButton.addEventListener('click', go);
  const link = (label, page, disabled = false, selected = false) => {
    const item = document.createElement('a');
    item.textContent = label;
    item.href = urlFor(page);
    if (disabled) { item.className = 'disabled'; item.setAttribute('aria-disabled', 'true'); item.tabIndex = -1; }
    if (selected) item.setAttribute('aria-current', 'page');
    return item;
  };

  controls.append(input, goButton, link('Previous', Math.max(1, current - 1), current === 1));
  pages.forEach(page => controls.appendChild(link(String(page), page, false, page === current)));
  controls.appendChild(link('Next', Math.min(totalPages, current + 1), current === totalPages));
  pager.replaceChildren(count, controls);
}

window.initBisCrsPagination = initBisCrsPagination;
document.addEventListener('DOMContentLoaded', initBisCrsPagination);
