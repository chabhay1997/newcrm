document.addEventListener('DOMContentLoaded', () => {
  const stats = document.querySelector('.tm-stats');
  const totalRecords = document.getElementById('tm-record-ids')?.dataset.totalTrademarks || '0';
  if (stats && !stats.querySelector('[data-status-card="Total"]')) {
    stats.insertAdjacentHTML('afterbegin', `<article data-status-card="Total"><i class="total-card">&Sigma;</i><div><b>Total</b><strong>${totalRecords}</strong><small>All trademarks</small></div></article>`);
  }
  document.querySelectorAll('.tm-flash').forEach(notification => {
    window.setTimeout(() => {
      notification.classList.add('is-hiding');
      notification.addEventListener('transitionend', () => notification.remove(), {once: true});
      window.setTimeout(() => notification.remove(), 400);
    }, 6000);
  });
  const dotsIcon = '<svg viewBox="0 0 24 24" fill="none" stroke="#5369ff" stroke-width="2.8" stroke-linecap="round" aria-hidden="true"><circle cx="5" cy="12" r="1" fill="currentColor"/><circle cx="12" cy="12" r="1" fill="currentColor"/><circle cx="19" cy="12" r="1" fill="currentColor"/></svg>';

  const importFile = document.getElementById('tm-import-file');
  importFile?.addEventListener('change', () => {
    if (!importFile.files?.length) return;
    const form = document.createElement('form');
    form.method = 'post'; form.action = '/operation/trademark/import'; form.enctype = 'multipart/form-data'; form.hidden = true;
    importFile.name = 'file';
    const csrf = document.createElement('input'); csrf.type = 'hidden'; csrf.name = '_csrf'; csrf.value = document.querySelector('meta[name="_csrf"]')?.content || '';
    form.append(importFile, csrf); document.body.appendChild(form); form.submit();
  });

  const prepareActionMenus = root => {
    const ids = [...document.querySelectorAll('#tm-record-ids [data-record-id]')].map(item => item.dataset.recordId);
    root.querySelectorAll('.tm-action-menu').forEach((menu, index) => {
      const trigger = menu.querySelector('.tm-menu-trigger'); if (trigger) trigger.innerHTML = dotsIcon;
      const companyLink = menu.closest('tr')?.querySelector('.company > a');
      if (companyLink && ids[index]) {
        companyLink.href = `/operation/trademark/${encodeURIComponent(ids[index])}/edit`;
        companyLink.title = 'Edit trademark details';
      }
      menu.querySelectorAll('.tm-menu a').forEach(item => item.remove());
      const deleteButton = menu.querySelector('.tm-delete-action');
      if (deleteButton && ids[index]) deleteButton.dataset.recordId = ids[index];
    });
  };

  const initPagination = () => {
    const pager = document.querySelector('.tm-footer.classic-pagination');
    if (!pager) return;
    const current = Number(pager.dataset.current), totalPages = Number(pager.dataset.pages), total = Number(pager.dataset.total), size = Number(pager.dataset.size);
    const first = total ? (current - 1) * size + 1 : 0, last = total ? Math.min(current * size, total) : 0;
    const urlFor = page => { const url = new URL(location.href); url.searchParams.set('page', page - 1); return url.pathname + url.search; };
    const count = document.createElement('span'); count.textContent = `Showing ${first} to ${last} of ${total} entries`;
    const controls = document.createElement('nav'); controls.className = 'classic-pagination-controls'; controls.setAttribute('aria-label', 'Trademark pages');
    const input = document.createElement('input'); input.type = 'number'; input.min = '1'; input.max = totalPages; input.placeholder = 'Go to...'; input.setAttribute('aria-label', 'Go to page');
    const go = () => { const page = Number(input.value); if (Number.isInteger(page) && page >= 1 && page <= totalPages) location.href = urlFor(page); else input.setCustomValidity(`Enter a page from 1 to ${totalPages}.`); };
    input.addEventListener('input', () => input.setCustomValidity(''));
    input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); go(); } });
    const goButton = document.createElement('button'); goButton.type = 'button'; goButton.className = 'go-button'; goButton.textContent = 'Go'; goButton.addEventListener('click', go);
    const link = (label, page, disabled = false, selected = false) => { const item = document.createElement('a'); item.textContent = label; item.href = urlFor(page); if (disabled) { item.className = 'disabled'; item.setAttribute('aria-disabled', 'true'); item.tabIndex = -1; } if (selected) item.setAttribute('aria-current', 'page'); return item; };
    controls.append(input, goButton, link('Previous', Math.max(1, current - 1), current === 1));
    for (let page = Math.max(1, current - 2); page <= Math.min(totalPages, current + 2); page++) controls.append(link(String(page), page, false, page === current));
    controls.append(link('Next', Math.min(totalPages, current + 1), current === totalPages));
    pager.replaceChildren(count, controls);
  };

  const searchControl = document.querySelector('.tm-filter .search-input');
  const searchInput = searchControl?.querySelector('input[name="search"]');
  const filterForm = document.querySelector('.tm-filter');
  const startDateFilter = filterForm?.elements.startDate;
  const endDateFilter = filterForm?.elements.endDate;
  const paymentFilter = filterForm?.elements.paymentStatus;
  if (paymentFilter && !paymentFilter.querySelector('option[value="Half"]')) {
    const half = new Option('Half', 'Half');
    const pending = paymentFilter.querySelector('option[value="Pending"]');
    paymentFilter.insertBefore(half, pending);
  }
  const exportLink = document.querySelector('.tm-excel-button.export');
  exportLink?.addEventListener('click', event => {
    event.preventDefault();
    const url = new URL('/operation/trademark/export', location.origin);
    ['search', 'startDate', 'endDate', 'paymentStatus'].forEach(name => {
      const value = filterForm.elements[name]?.value?.trim(); if (value) url.searchParams.set(name, value);
    });
    const selectedStatus = new URL(location.href).searchParams.get('status');
    if (selectedStatus) url.searchParams.set('status', selectedStatus);
    location.href = url.pathname + url.search;
  });
  let suggestions;
  if (searchControl && searchInput) {
    [...searchControl.childNodes].filter(node => node.nodeType === Node.TEXT_NODE).forEach(node => node.remove());
    searchControl.insertAdjacentHTML('afterbegin', '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4.5 4.5"/></svg>');
    suggestions = document.createElement('div'); suggestions.className = 'tm-search-suggestions'; suggestions.setAttribute('role', 'listbox'); suggestions.hidden = true;
    searchControl.appendChild(suggestions);
  }

  const renderSuggestions = rows => {
    if (!suggestions || !searchInput.value.trim()) { if (suggestions) suggestions.hidden = true; return; }
    const matches = [];
    rows.forEach(row => {
      const company = row.querySelector('.company a')?.textContent.trim();
      const details = row.cells[2]?.textContent.replace(/\s+/g, ' ').trim();
      if (company && company !== 'N/A' && !matches.some(item => item.company === company)) matches.push({company, details});
    });
    suggestions.replaceChildren();
    matches.slice(0, 6).forEach(match => {
      const button = document.createElement('button'); button.type = 'button'; button.setAttribute('role', 'option');
      const name = document.createElement('strong'); name.textContent = match.company;
      const detail = document.createElement('small'); detail.textContent = match.details || 'Trademark application';
      button.append(name, detail);
      button.addEventListener('mousedown', event => event.preventDefault());
      button.addEventListener('click', () => { searchInput.value = match.company; suggestions.hidden = true; loadResults(); });
      suggestions.appendChild(button);
    });
    suggestions.hidden = !matches.length;
  };

  let timer, request;
  const loadResults = async () => {
    const term = searchInput.value.trim();
    const url = new URL(location.href);
    url.searchParams.set('page', '0');
    if (term) url.searchParams.set('search', term); else url.searchParams.delete('search');
    ['startDate', 'endDate', 'paymentStatus', 'size'].forEach(name => {
      const field = filterForm.elements[name];
      if (field?.value) url.searchParams.set(name, field.value); else url.searchParams.delete(name);
    });
    request?.abort(); request = new AbortController();
    const tableWrap = document.querySelector('.tm-table-wrap'); tableWrap?.setAttribute('aria-busy', 'true');
    try {
      const response = await fetch(url.pathname + url.search, {signal: request.signal, headers: {'X-Requested-With': 'XMLHttpRequest'}});
      if (!response.ok) throw new Error('Search failed');
      const page = new DOMParser().parseFromString(await response.text(), 'text/html');
      const nextBody = page.querySelector('.tm-table-wrap tbody'), currentBody = document.querySelector('.tm-table-wrap tbody');
      const nextFooter = page.querySelector('.tm-footer'), currentFooter = document.querySelector('.tm-footer');
      const nextIds = page.querySelector('#tm-record-ids'), currentIds = document.querySelector('#tm-record-ids');
      if (!nextBody || !currentBody || !nextFooter || !currentFooter || !nextIds || !currentIds) throw new Error('Invalid search response');
      currentBody.replaceWith(nextBody); currentFooter.replaceWith(nextFooter); currentIds.replaceWith(nextIds);
      prepareActionMenus(nextBody); initPagination(); renderSuggestions([...nextBody.rows]);
      history.replaceState(null, '', url.pathname + url.search);
    } catch (error) {
      if (error.name !== 'AbortError') { suggestions.replaceChildren(); suggestions.hidden = true; }
    } finally { tableWrap?.removeAttribute('aria-busy'); }
  };

  const statusCards = [...document.querySelectorAll('.tm-stats article')];
  const statusForCard = card => card.querySelector('b')?.textContent.trim() || '';
  const showActiveStatus = () => {
    const selected = new URL(location.href).searchParams.get('status') || '';
    statusCards.forEach(card => {
      const cardStatus = statusForCard(card);
      const active = cardStatus === 'Total' ? !selected : cardStatus.toLowerCase() === selected.toLowerCase();
      card.classList.toggle('active-filter', active);
      card.setAttribute('aria-pressed', String(active));
    });
  };
  statusCards.forEach(card => {
    card.tabIndex = 0; card.setAttribute('role', 'button'); card.setAttribute('aria-label', `Filter by ${statusForCard(card)} status`);
    const choose = () => {
      const url = new URL(location.href);
      if (statusForCard(card) === 'Total') url.searchParams.delete('status'); else url.searchParams.set('status', statusForCard(card));
      url.searchParams.set('page', '0');
      history.replaceState(null, '', url.pathname + url.search); showActiveStatus(); loadResults();
    };
    card.addEventListener('click', choose);
    card.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); choose(); } });
  });
  const rowSize = filterForm?.elements.size;
  if (rowSize) { rowSize.removeAttribute('onchange'); rowSize.addEventListener('change', loadResults); }
  const applyFieldFilters = () => {
    if (startDateFilter && endDateFilter) {
      startDateFilter.max = endDateFilter.value || '';
      endDateFilter.min = startDateFilter.value || '';
      const invalidRange = startDateFilter.value && endDateFilter.value && startDateFilter.value > endDateFilter.value;
      endDateFilter.setCustomValidity(invalidRange ? 'End Date must be on or after Start Date.' : '');
      if (invalidRange) { endDateFilter.reportValidity(); return; }
    }
    loadResults();
  };
  startDateFilter?.addEventListener('change', applyFieldFilters);
  endDateFilter?.addEventListener('change', applyFieldFilters);
  paymentFilter?.addEventListener('change', applyFieldFilters);
  showActiveStatus();

  const clearFilter = document.querySelector('.tm-clear');
  clearFilter?.addEventListener('click', event => {
    event.preventDefault();
    clearTimeout(timer);
    ['search', 'startDate', 'endDate', 'paymentStatus'].forEach(name => {
      const field = filterForm?.elements[name]; if (field) field.value = '';
    });
    if (startDateFilter) startDateFilter.removeAttribute('max');
    if (endDateFilter) { endDateFilter.removeAttribute('min'); endDateFilter.setCustomValidity(''); }
    if (rowSize) rowSize.value = '25';
    if (suggestions) { suggestions.replaceChildren(); suggestions.hidden = true; }
    history.replaceState(null, '', '/operation/trademark');
    showActiveStatus();
    loadResults();
  });

  searchInput?.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(loadResults, 220); });
  searchInput?.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); clearTimeout(timer); loadResults(); } if (event.key === 'Escape') suggestions.hidden = true; });
  searchInput?.addEventListener('focus', () => { if (suggestions?.childElementCount && searchInput.value.trim()) suggestions.hidden = false; });
  document.addEventListener('click', event => { if (searchControl && !searchControl.contains(event.target)) suggestions.hidden = true; });

  prepareActionMenus(document); initPagination();
  document.addEventListener('click', event => {
    const deleteButton = event.target.closest('.tm-delete-action[data-record-id]');
    if (deleteButton) {
      event.preventDefault();
      if (!confirm('Permanently delete this trademark record? This action cannot be undone.')) return;
      const form = document.createElement('form'); form.method = 'post'; form.action = `/operation/trademark/${deleteButton.dataset.recordId}/delete`; form.hidden = true;
      const csrf = document.createElement('input'); csrf.type = 'hidden'; csrf.name = '_csrf'; csrf.value = document.querySelector('meta[name="_csrf"]')?.content || '';
      form.appendChild(csrf); document.body.appendChild(form); form.submit(); return;
    }
    const trigger = event.target.closest('.tm-menu-trigger');
    document.querySelectorAll('.tm-action-menu.open').forEach(menu => { if (menu !== trigger?.closest('.tm-action-menu')) { menu.classList.remove('open'); menu.querySelector('.tm-menu-trigger').setAttribute('aria-expanded', 'false'); } });
    if (trigger) {
      const menu = trigger.closest('.tm-action-menu');
      const open = menu.classList.toggle('open');
      trigger.setAttribute('aria-expanded', String(open));
      const panel = menu.querySelector('.tm-menu');
      if (open && panel) {
        const triggerBox = trigger.getBoundingClientRect();
        const menuWidth = 144;
        const estimatedHeight = panel.scrollHeight || 42;
        const left = Math.max(8, Math.min(window.innerWidth - menuWidth - 8, triggerBox.right - menuWidth));
        const roomBelow = window.innerHeight - triggerBox.bottom;
        const top = roomBelow >= estimatedHeight + 8 ? triggerBox.bottom + 5 : Math.max(8, triggerBox.top - estimatedHeight - 5);
        panel.style.left = `${left}px`; panel.style.top = `${top}px`; panel.style.right = 'auto';
      }
    }
  });
});
