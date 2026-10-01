document.addEventListener('DOMContentLoaded', () => {
  const statsToggle = document.getElementById('fmcs-one-stats-toggle');
  const analytics = document.getElementById('fmcs-one-analytics');
  const setAnalyticsVisible = visible => {
    if (!analytics || !statsToggle) return;
    analytics.hidden = !visible;
    statsToggle.setAttribute('aria-expanded', String(visible));
    statsToggle.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V10M10 20V4M16 20v-7M22 20V7"/></svg>${visible ? 'Hide Analytics' : 'View Analytics'}`;
  };
  statsToggle?.addEventListener('click', () => {
    const visible = analytics.hidden;
    setAnalyticsVisible(visible);
    showTransientAlert(visible ? 'FMCS analytics opened.' : 'FMCS analytics hidden.');
  });

  const analyticsYear = document.getElementById('fmcs-one-analytics-year');
  const yearToggle = document.getElementById('fmcs-one-year-toggle');
  const yearOptions = document.getElementById('fmcs-one-year-options');
  let analyticsRequestVersion = 0;
  const closeYearOptions = () => {
    if (!yearOptions || !yearToggle) return;
    yearOptions.hidden = true;
    yearToggle.setAttribute('aria-expanded', 'false');
  };
  yearToggle?.addEventListener('click', () => {
    const opening = yearOptions.hidden;
    yearOptions.hidden = !opening;
    yearToggle.setAttribute('aria-expanded', String(opening));
  });
  yearOptions?.addEventListener('click', event => {
    const option = event.target.closest('[role="option"]');
    if (!option) return;
    analyticsYear.value = option.dataset.value;
    yearToggle.querySelector('span').textContent = option.textContent;
    yearOptions.querySelectorAll('[role="option"]').forEach(item => item.setAttribute('aria-selected', String(item === option)));
    closeYearOptions();
    updateAnalytics();
  });
  document.addEventListener('click', event => {
    if (!event.target.closest('.fmcs-one-country-select')) closeYearOptions();
  });
  const updateAnalytics = async () => {
    if (!analytics || !analyticsYear) return;
    const requestVersion = ++analyticsRequestVersion;
    [analyticsYear, yearToggle].forEach(control => { if (control) control.disabled = true; });
    try {
      const query = new URLSearchParams({ year: analyticsYear.value });
      const response = await fetch('/operation/fmcs-1/analytics?' + query, {
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error('Unable to load FMCS analytics');
      const data = await response.json();
      if (requestVersion !== analyticsRequestVersion) return;
      const maximum = Math.max(data.maximum || 0, 1);
      const axis = analytics.querySelectorAll('.fmcs-one-y-axis span');
      [data.maximum || 0, Math.floor((data.maximum || 0) * 3 / 4), Math.floor((data.maximum || 0) / 2), Math.floor((data.maximum || 0) / 4), 0]
        .forEach((value, index) => { if (axis[index]) axis[index].textContent = value; });
      const bars = analytics.querySelector('.fmcs-one-bars');
      bars.replaceChildren();
      data.countries.forEach(item => {
        const barItem = document.createElement('div');
        barItem.className = 'fmcs-one-bar-item';
        if (item.count > 0) {
          const value = document.createElement('span');
          value.className = 'fmcs-one-bar-value';
          value.style.bottom = `calc(${item.count * 100 / maximum}% + 4px)`;
          value.textContent = item.count;
          barItem.append(value);
        }
        const bar = document.createElement('span');
        bar.className = 'fmcs-one-bar';
        bar.style.height = `${item.count * 100 / maximum}%`;
        bar.title = `${item.country}: ${item.count} applications`;
        const label = document.createElement('span');
        label.className = 'fmcs-one-bar-label';
        label.textContent = item.country;
        barItem.append(bar, label);
        bars.append(barItem);
      });
      analytics.querySelector('.fmcs-one-x-title').textContent = data.xAxisTitle;
      const url = new URL(window.location.href);
      url.searchParams.set('analyticsYear', data.year);
      url.searchParams.delete('analyticsCountry');
      url.searchParams.delete('analyticsType');
      window.history.replaceState({}, '', url);
      showTransientAlert('Analytics filters updated successfully.');
    } catch (error) {
      if (requestVersion === analyticsRequestVersion) {
        console.error(error);
        showTransientAlert('Unable to update FMCS analytics.', 'error');
      }
    } finally {
      if (requestVersion === analyticsRequestVersion)
        [analyticsYear, yearToggle].forEach(control => { if (control) control.disabled = false; });
    }
  };
  analyticsYear?.addEventListener('change', updateAnalytics);

  const search = document.getElementById('fmcs-one-search');
  const searchForm = search?.form;
  const suggestions = document.getElementById('fmcs-one-search-suggestions');
  let searchTimer;
  let suggestionTimer;
  let suggestionRequest;
  let activeSuggestion = -1;
  const closeSuggestions = () => {
    if (!suggestions || !search) return;
    suggestions.hidden = true;
    search.setAttribute('aria-expanded', 'false');
    activeSuggestion = -1;
  };
  const selectSuggestion = value => {
    search.value = value;
    closeSuggestions();
    searchForm.requestSubmit();
  };
  search?.addEventListener('input', () => {
    window.clearTimeout(searchTimer);
    window.clearTimeout(suggestionTimer);
    suggestionRequest?.abort();
    closeSuggestions();
    const query = search.value.trim();
    if (!query) {
      searchTimer = window.setTimeout(() => searchForm.requestSubmit(), 250);
      return;
    }
    suggestionTimer = window.setTimeout(async () => {
      const request = new AbortController();
      suggestionRequest = request;
      try {
        const response = await fetch('/operation/fmcs-1/suggestions?query=' + encodeURIComponent(query), { signal: request.signal });
        if (!response.ok) throw new Error();
        const items = await response.json();
        if (search.value.trim() !== query) return;
        suggestions.replaceChildren();
        items.forEach((item, index) => {
          const option = document.createElement('div');
          option.id = 'fmcs-one-suggestion-' + index;
          option.className = 'fmcs-one-search-suggestion';
          option.setAttribute('role', 'option');
          const value = document.createElement('strong'); value.textContent = item.value;
          const detail = document.createElement('small'); detail.textContent = item.detail;
          option.append(value, detail);
          option.addEventListener('mousedown', event => event.preventDefault());
          option.addEventListener('click', () => selectSuggestion(item.value));
          suggestions.appendChild(option);
        });
        suggestions.hidden = !items.length;
        search.setAttribute('aria-expanded', String(items.length > 0));
      } catch (error) { if (error.name !== 'AbortError') closeSuggestions(); }
    }, 140);
    searchTimer = window.setTimeout(() => searchForm.requestSubmit(), 650);
  });

  search?.addEventListener('keydown', event => {
    const options = [...(suggestions?.querySelectorAll('[role="option"]') || [])];
    if (event.key === 'Escape') return closeSuggestions();
    if (suggestions?.hidden || !options.length) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      activeSuggestion = event.key === 'ArrowDown' ? (activeSuggestion + 1) % options.length : (activeSuggestion - 1 + options.length) % options.length;
      options.forEach((option, index) => option.classList.toggle('active', index === activeSuggestion));
    } else if (event.key === 'Enter' && activeSuggestion >= 0) {
      event.preventDefault(); selectSuggestion(options[activeSuggestion].querySelector('strong').textContent);
    }
  });
  document.addEventListener('click', event => { if (searchForm && !searchForm.contains(event.target)) closeSuggestions(); });

  const buildPagination = pager => {
    if (!pager) return;
    const current = Number(pager.dataset.current);
    const pages = Number(pager.dataset.pages);
    const controls = pager.querySelector('.classic-pagination-controls');
    controls.replaceChildren();
    const urlFor = page => {
      const url = new URL(location.href);
      url.searchParams.set('page', page - 1);
      return url.pathname + url.search;
    };
    const link = (label, page, disabled, selected) => {
      const item = document.createElement('a');
      item.textContent = label;
      item.href = urlFor(page);
      if (disabled) { item.className = 'disabled'; item.setAttribute('aria-disabled', 'true'); }
      if (selected) item.setAttribute('aria-current', 'page');
      return item;
    };
    const input = document.createElement('input');
    input.type = 'number'; input.min = '1'; input.max = String(pages); input.placeholder = 'Go to...';
    input.setAttribute('aria-label', 'Go to page');
    const go = () => {
      const page = Number(input.value);
      if (Number.isInteger(page) && page >= 1 && page <= pages) location.href = urlFor(page);
      else input.setCustomValidity(`Enter a page from 1 to ${pages}.`);
    };
    input.addEventListener('input', () => input.setCustomValidity(''));
    input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); go(); } });
    const goButton = document.createElement('button');
    goButton.type = 'button'; goButton.className = 'go-button'; goButton.textContent = 'Go';
    goButton.addEventListener('click', go);
    controls.append(input, goButton, link('Previous', Math.max(1, current - 1), current === 1, false));
    for (let page = Math.max(1, current - 2); page <= Math.min(pages, current + 2); page++)
      controls.append(link(String(page), page, false, page === current));
    controls.append(link('Next', Math.min(pages, current + 1), current === pages, false));
  };
  buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));

  const refreshTable = async (form, changedControl) => {
    const params = new URLSearchParams(new FormData(form));
    params.set('page', '0');
    const url = '/operation/fmcs-1?' + params;
    changedControl.disabled = true;
    try {
      const response = await fetch(url, { headers: { 'X-Requested-With': 'XMLHttpRequest' } });
      if (!response.ok) throw new Error('Unable to filter FMCS records');
      const documentResult = new DOMParser().parseFromString(await response.text(), 'text/html');
      const replacementTable = documentResult.querySelector('.fmcs-one-table-wrap');
      const replacementPager = documentResult.querySelector('.fmcs-one-footer.classic-pagination');
      document.querySelector('.fmcs-one-table-wrap').replaceWith(replacementTable);
      document.querySelector('.fmcs-one-footer.classic-pagination').replaceWith(replacementPager);
      document.querySelectorAll('.fmcs-one-table-toolbar [name]').forEach(control => {
        if (params.has(control.name)) control.value = params.get(control.name) || '';
      });
      window.history.replaceState({}, '', url);
      buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));
      const labels = {client: 'Client filter applied.', payment: 'Payment filter applied.', status: 'Status filter applied.', startDate: 'Start-date filter applied.', endDate: 'End-date filter applied.', size: 'Table row count updated.'};
      if (changedControl.name !== 'search') showTransientAlert(labels[changedControl.name] || 'Filters applied successfully.');
    } catch (error) {
      console.error(error);
      if (changedControl.name !== 'search') showTransientAlert('Unable to apply the selected filters.', 'error');
    } finally {
      changedControl.disabled = false;
    }
  };
  let cardRequest;
  document.querySelectorAll('.fmcs-one-status-card').forEach(card => {
    card.addEventListener('click', async event => {
      event.preventDefault();
      if (cardRequest) cardRequest.abort();
      cardRequest = new AbortController();
      const url = new URL(card.href, window.location.origin);
      url.searchParams.set('page', '0');
      const cards = document.querySelectorAll('.fmcs-one-status-card');
      cards.forEach(item => item.setAttribute('aria-busy', 'true'));
      try {
        const response = await fetch(url, {
          headers: { 'X-Requested-With': 'XMLHttpRequest' },
          signal: cardRequest.signal
        });
        if (!response.ok) throw new Error('Unable to filter FMCS records');
        const documentResult = new DOMParser().parseFromString(await response.text(), 'text/html');
        const replacementTable = documentResult.querySelector('.fmcs-one-table-wrap');
        const replacementPager = documentResult.querySelector('.fmcs-one-footer.classic-pagination');
        if (!replacementTable || !replacementPager) throw new Error('Incomplete FMCS filter response');
        document.querySelector('.fmcs-one-table-wrap').replaceWith(replacementTable);
        document.querySelector('.fmcs-one-footer.classic-pagination').replaceWith(replacementPager);
        cards.forEach(item => {
          const selected = item === card;
          item.classList.toggle('selected', selected);
          item.setAttribute('aria-pressed', String(selected));
        });
        document.querySelectorAll('.fmcs-one-table-toolbar [name="card"]').forEach(input => { input.value = url.searchParams.get('card') || ''; });
        window.history.replaceState({}, '', url.pathname + url.search);
        buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));
        showTransientAlert(`${card.querySelector('b')?.textContent?.trim() || 'Status'} filter applied.`);
      } catch (error) {
        if (error.name !== 'AbortError') window.location.assign(url);
      } finally {
        cards.forEach(item => item.removeAttribute('aria-busy'));
      }
    });
  });
  document.querySelectorAll('.fmcs-one-header-select-form, .fmcs-one-filter-form, .fmcs-one-search-form').forEach(form => {
    form.addEventListener('submit', event => {
      event.preventDefault();
      const changedControl = document.activeElement?.form === form ? document.activeElement : (form.querySelector('[name="search"]') || form);
      refreshTable(form, changedControl);
    });
    form.querySelectorAll('select').forEach(select => select.addEventListener('change', () => refreshTable(form, select)));
  });

  const showTransientAlert = (message, type = 'success') => {
    document.querySelectorAll('.fmcs-one-toast').forEach(alert => alert.remove());
    const alert = document.createElement('div');
    alert.className = `fmcs-one-toast ${type}`;
    alert.setAttribute(type === 'error' ? 'role' : 'status', type === 'error' ? 'alert' : 'status');
    alert.innerHTML = `<span aria-hidden="true">${type === 'error' ? '!' : '✓'}</span><p></p>`;
    alert.querySelector('p').textContent = message;
    document.body.append(alert);
    requestAnimationFrame(() => alert.classList.add('visible'));
    window.setTimeout(() => {
      alert.classList.remove('visible');
      window.setTimeout(() => alert.remove(), 180);
    }, 2000);
  };
  document.querySelectorAll('.fmcs-one-page>.fmcs-one-alert').forEach(alert => {
    const message = alert.textContent.trim();
    const type = alert.classList.contains('error') ? 'error' : 'success';
    alert.remove();
    showTransientAlert(message, type);
  });
  const pendingToast = sessionStorage.getItem('fmcsToast');
  if (pendingToast) {
    sessionStorage.removeItem('fmcsToast');
    showTransientAlert(pendingToast);
  }
  document.addEventListener('click', async event => {
    const clear = event.target.closest('.fmcs-one-clear-filters');
    if (clear) {
      event.preventDefault();
      try {
        const response = await fetch(clear.href, {headers: {'X-Requested-With': 'XMLHttpRequest'}});
        if (!response.ok) throw new Error('Unable to clear filters.');
        const result = new DOMParser().parseFromString(await response.text(), 'text/html');
        const replacementTable = result.querySelector('.fmcs-one-table-wrap');
        const replacementPager = result.querySelector('.fmcs-one-footer.classic-pagination');
        document.querySelector('.fmcs-one-table-wrap').replaceWith(replacementTable);
        document.querySelector('.fmcs-one-footer.classic-pagination').replaceWith(replacementPager);
        document.querySelectorAll('.fmcs-one-status-card').forEach(card => {
          card.classList.remove('selected');
          card.setAttribute('aria-pressed', 'false');
        });
        document.querySelectorAll('.fmcs-one-table-toolbar input:not([type="file"]):not([name="size"]), .fmcs-one-table-toolbar select:not([name="size"])').forEach(control => { control.value = ''; });
        document.querySelectorAll('.fmcs-one-table-toolbar [name="size"]').forEach(control => { control.value = '25'; });
        window.history.replaceState({}, '', '/operation/fmcs-1');
        buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));
        showTransientAlert('All FMCS filters cleared.');
      } catch (error) {
        showTransientAlert(error.message, 'error');
      }
      return;
    }
    if (event.target.closest('.fmcs-one-excel-button.export')) showTransientAlert('FMCS export started.');
    if (event.target.closest('.fmcs-one-excel-button.import')) showTransientAlert('Choose an Excel file to import.');
    if (event.target.closest('.fmcs-one-add-button')) showTransientAlert('Opening the Add FMCS form.');
    const pageControl = event.target.closest('.classic-pagination-controls a, .classic-pagination-controls .go-button');
    if (pageControl && !pageControl.classList.contains('disabled')) sessionStorage.setItem('fmcsToast', 'FMCS page changed successfully.');
  });
  document.addEventListener('submit', async event => {
    const form = event.target.closest('.fmcs-one-row-actions form');
    if (!form || event.defaultPrevented) return;
    event.preventDefault();
    const deleteButton = form.querySelector('.fmcs-one-row-action.delete');
    deleteButton.disabled = true;
    try {
      const deleteResponse = await fetch(form.action, {
        method: 'POST',
        body: new FormData(form),
        headers: {'X-Requested-With': 'XMLHttpRequest'}
      });
      if (!deleteResponse.ok) throw new Error('Unable to delete the FMCS operation.');
      const deleteDocument = new DOMParser().parseFromString(await deleteResponse.text(), 'text/html');
      const serverAlert = deleteDocument.querySelector('.fmcs-one-alert');
      const message = serverAlert?.textContent?.trim() || 'FMCS operation deleted successfully.';
      const type = serverAlert?.classList.contains('error') ? 'error' : 'success';
      const tableResponse = await fetch(window.location.href, {headers: {'X-Requested-With': 'XMLHttpRequest'}});
      if (!tableResponse.ok) throw new Error('The record was deleted, but the table could not be refreshed.');
      const tableDocument = new DOMParser().parseFromString(await tableResponse.text(), 'text/html');
      const replacementTable = tableDocument.querySelector('.fmcs-one-table-wrap');
      const replacementPager = tableDocument.querySelector('.fmcs-one-footer.classic-pagination');
      if (!replacementTable || !replacementPager) throw new Error('The refreshed FMCS table is incomplete.');
      document.querySelector('.fmcs-one-table-wrap').replaceWith(replacementTable);
      document.querySelector('.fmcs-one-footer.classic-pagination').replaceWith(replacementPager);
      buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));
      showTransientAlert(message, type);
    } catch (error) {
      deleteButton.disabled = false;
      showTransientAlert(error.message, 'error');
    }
  });

  const previewModal = document.querySelector('#fmcs-preview-modal');
  const previewLoading = previewModal?.querySelector('.fmcs-preview-loading');
  const previewContent = previewModal?.querySelector('.fmcs-preview-content');
  const previewEditForm = previewModal?.querySelector('#fmcs-preview-edit-form');
  let activePreviewId = null;
  const displayPreviewValue = value => {
    if (value === null || value === undefined || String(value).trim() === '') return 'N/A';
    if (Array.isArray(value) && value.length >= 3) return `${String(value[2]).padStart(2, '0')}-${String(value[1]).padStart(2, '0')}-${value[0]}`;
    const text = String(value).trim();
    const isoDate = text.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s].*)?$/);
    return isoDate ? `${isoDate[3]}-${isoDate[2]}-${isoDate[1]}` : text;
  };
  const syncPaymentValue = name => {
    if (!previewModal) return;
    const selected = [...previewModal.querySelectorAll(`[data-payment-choice="${name}"]:checked`)].map(input => input.value);
    const hidden = previewModal.querySelector(`[data-payment-value="${name}"]`);
    if (hidden) hidden.value = selected.join(',');
  };
  const openPreview = async (id, announce = true) => {
    if (!previewModal) return;
    activePreviewId = id;
    previewLoading.textContent = 'Loading FMCS details…';
    previewLoading.hidden = false;
    previewContent.hidden = true;
    if (!previewModal.open) previewModal.showModal();
    try {
      const response = await fetch(`/operation/fmcs-1/${encodeURIComponent(id)}/preview-data`, {
        headers: {Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest'}
      });
      if (!response.ok) throw new Error('Unable to load this FMCS operation.');
      const record = await response.json();
      previewModal.querySelectorAll('[data-preview]').forEach(element => {
        element.textContent = displayPreviewValue(record[element.dataset.preview]);
        element.title = element.textContent;
      });
      previewModal.querySelectorAll('[data-preview-input]').forEach(control => {
        const key = control.dataset.previewInput;
        if (key === 'country_id') {
          const countries = Array.isArray(record.country_options) ? record.country_options : [];
          control.replaceChildren(new Option('--select--', ''), ...countries.map(country => new Option(country.name, country.id)));
        } else if (key === 'license_status') {
          const statuses = [
            ['', '--select--'], ['1', 'Fresh Project'], ['2', 'Docs Review'],
            ['3', 'Document Submit To BIS'], ['4', 'Application No.'], ['6', 'Nomination Pending'],
            ['7', 'Nomination Done'], ['8', 'Inspection Pending'], ['14', 'Inspection Done'],
            ['15', 'License Granted'], ['16', 'Project Hold'], ['18', 'PBG Done'],
            ['19', 'SIT Done'], ['20', 'Payment Status']
          ];
          control.replaceChildren(...statuses.map(([value, label]) => new Option(label, value)));
        }
        control.value = record[key] === null || record[key] === undefined ? '' : String(record[key]);
      });
      ['service_fee', 'bis_pay'].forEach(name => {
        const selected = new Set(String(record[name] || '').split(',').map(value => value.trim()).filter(Boolean));
        previewModal.querySelectorAll(`[data-payment-choice="${name}"]`).forEach(input => {
          input.checked = selected.has(input.value);
        });
        syncPaymentValue(name);
      });
      const saveMessage = previewModal.querySelector('.fmcs-preview-save-message');
      saveMessage.textContent = '';
      saveMessage.classList.remove('error');
      const files = ['pbg_upload', 'certificate_upload', 'upload', 'uploads']
        .map(key => record[key]).filter(value => value !== null && value !== undefined && String(value).trim() !== '');
      const filesElement = previewModal.querySelector('[data-preview-files]');
      filesElement.textContent = files.length ? files.join(', ') : 'No files available';
      filesElement.title = filesElement.textContent;
      previewLoading.hidden = true;
      previewContent.hidden = false;
      if (announce) showTransientAlert('FMCS preview loaded.');
    } catch (error) {
      previewLoading.textContent = error.message;
      showTransientAlert(error.message, 'error');
    }
  };
  document.addEventListener('click', event => {
    const previewButton = event.target.closest('[data-preview-id]');
    if (previewButton) openPreview(previewButton.dataset.previewId);
  });
  previewModal?.querySelector('.fmcs-preview-close')?.addEventListener('click', () => {
    previewModal.close();
    showTransientAlert('FMCS preview closed.');
  });
  previewModal?.addEventListener('click', event => {
    if (event.target === previewModal) {
      previewModal.close();
      showTransientAlert('FMCS preview closed.');
    }
  });
  previewModal?.addEventListener('change', event => {
    const paymentChoice = event.target.closest('[data-payment-choice]');
    if (paymentChoice) {
      syncPaymentValue(paymentChoice.dataset.paymentChoice);
      showTransientAlert('Payment selection updated. Save changes to store it.');
    }
  });
  previewEditForm?.addEventListener('submit', async event => {
    event.preventDefault();
    if (!activePreviewId) return;
    const saveButton = previewEditForm.querySelector('.fmcs-preview-save');
    const saveMessage = previewEditForm.querySelector('.fmcs-preview-save-message');
    saveButton.disabled = true;
    syncPaymentValue('service_fee');
    syncPaymentValue('bis_pay');
    saveMessage.classList.remove('error');
    saveMessage.textContent = 'Saving changes…';
    try {
      const formData = new FormData(previewEditForm);
      const requestBody = new URLSearchParams();
      formData.forEach((value, key) => requestBody.append(key, String(value)));
      const csrfInput = previewEditForm.querySelector('input[type="hidden"][name]');
      const requestHeaders = {
        'Accept': 'application/json',
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        'X-Requested-With': 'XMLHttpRequest'
      };
      if (csrfInput?.value) requestHeaders['X-CSRF-TOKEN'] = csrfInput.value;
      const response = await fetch(`/operation/fmcs-1/${encodeURIComponent(activePreviewId)}/update`, {
        method: 'POST',
        credentials: 'same-origin',
        headers: requestHeaders,
        body: requestBody
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(result.error || 'Unable to save the FMCS operation.');
      saveMessage.textContent = result.message || 'Changes saved successfully.';
      showTransientAlert(result.message || 'FMCS changes saved successfully.');
      const tableResponse = await fetch(window.location.href, {
        credentials: 'same-origin',
        headers: {'X-Requested-With': 'XMLHttpRequest'}
      });
      if (tableResponse.ok) {
        const tableDocument = new DOMParser().parseFromString(await tableResponse.text(), 'text/html');
        const replacementTable = tableDocument.querySelector('.fmcs-one-table-wrap');
        const replacementPager = tableDocument.querySelector('.fmcs-one-footer.classic-pagination');
        if (replacementTable) document.querySelector('.fmcs-one-table-wrap')?.replaceWith(replacementTable);
        if (replacementPager) {
          document.querySelector('.fmcs-one-footer.classic-pagination')?.replaceWith(replacementPager);
          buildPagination(document.querySelector('.fmcs-one-footer.classic-pagination'));
        }
      }
      await openPreview(activePreviewId, false);
      saveMessage.textContent = result.message || 'Changes saved successfully.';
      saveButton.disabled = false;
    } catch (error) {
      saveMessage.classList.add('error');
      saveMessage.textContent = error.message;
      showTransientAlert(error.message, 'error');
      saveButton.disabled = false;
    }
  });
});
