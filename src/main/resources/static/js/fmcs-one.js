document.addEventListener('DOMContentLoaded', () => {
  const statsToggle = document.getElementById('fmcs-one-stats-toggle');
  const analytics = document.getElementById('fmcs-one-analytics');
  const setAnalyticsVisible = visible => {
    if (!analytics || !statsToggle) return;
    analytics.hidden = !visible;
    statsToggle.setAttribute('aria-expanded', String(visible));
    statsToggle.innerHTML = `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V10M10 20V4M16 20v-7M22 20V7"/></svg>${visible ? 'Hide Analytics' : 'View Analytics'}`;
  };
  statsToggle?.addEventListener('click', () => setAnalyticsVisible(analytics.hidden));

  const analyticsYear = document.getElementById('fmcs-one-analytics-year');
  const analyticsCountry = document.getElementById('fmcs-one-analytics-country');
  const analyticsType = document.getElementById('fmcs-one-analytics-type');
  const analyticsReset = document.getElementById('fmcs-one-analytics-reset');
  const countryToggle = document.getElementById('fmcs-one-country-toggle');
  const countryOptions = document.getElementById('fmcs-one-country-options');
  const typeToggle = document.getElementById('fmcs-one-type-toggle');
  const typeOptions = document.getElementById('fmcs-one-type-options');
  const yearToggle = document.getElementById('fmcs-one-year-toggle');
  const yearOptions = document.getElementById('fmcs-one-year-options');
  let analyticsRequestVersion = 0;
  const closeCountryOptions = () => {
    if (!countryOptions || !countryToggle) return;
    countryOptions.hidden = true;
    countryToggle.setAttribute('aria-expanded', 'false');
  };
  countryToggle?.addEventListener('click', () => {
    const opening = countryOptions.hidden;
    if (opening) { closeTypeOptions(); closeYearOptions(); }
    countryOptions.hidden = !opening;
    countryToggle.setAttribute('aria-expanded', String(opening));
  });
  countryOptions?.addEventListener('click', event => {
    const option = event.target.closest('[role="option"]');
    if (!option) return;
    analyticsCountry.value = option.dataset.value;
    countryToggle.querySelector('span').textContent = option.textContent;
    countryOptions.querySelectorAll('[role="option"]').forEach(item => item.setAttribute('aria-selected', String(item === option)));
    closeCountryOptions();
    updateAnalytics();
  });
  const closeTypeOptions = () => {
    if (!typeOptions || !typeToggle) return;
    typeOptions.hidden = true;
    typeToggle.setAttribute('aria-expanded', 'false');
  };
  typeToggle?.addEventListener('click', () => {
    const opening = typeOptions.hidden;
    if (opening) { closeCountryOptions(); closeYearOptions(); }
    typeOptions.hidden = !opening;
    typeToggle.setAttribute('aria-expanded', String(opening));
  });
  typeOptions?.addEventListener('click', event => {
    const option = event.target.closest('[role="option"]');
    if (!option) return;
    analyticsType.value = option.dataset.value;
    typeToggle.querySelector('span').textContent = option.textContent;
    typeOptions.querySelectorAll('[role="option"]').forEach(item => item.setAttribute('aria-selected', String(item === option)));
    closeTypeOptions();
    updateAnalytics();
  });
  const closeYearOptions = () => {
    if (!yearOptions || !yearToggle) return;
    yearOptions.hidden = true;
    yearToggle.setAttribute('aria-expanded', 'false');
  };
  yearToggle?.addEventListener('click', () => {
    const opening = yearOptions.hidden;
    if (opening) { closeCountryOptions(); closeTypeOptions(); }
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
    if (!event.target.closest('.fmcs-one-country-select')) closeCountryOptions();
    if (!event.target.closest('.fmcs-one-country-select')) closeTypeOptions();
    if (!event.target.closest('.fmcs-one-country-select')) closeYearOptions();
  });
  const updateAnalytics = async () => {
    if (!analytics || !analyticsYear || !analyticsCountry || !analyticsType) return;
    const requestVersion = ++analyticsRequestVersion;
    [analyticsYear, analyticsCountry, analyticsType, analyticsReset, countryToggle, typeToggle, yearToggle].forEach(control => { if (control) control.disabled = true; });
    try {
      const query = new URLSearchParams({ year: analyticsYear.value });
      if (analyticsCountry.value) query.set('country', analyticsCountry.value);
      if (analyticsType.value) query.set('type', analyticsType.value);
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
      if (analyticsCountry.value) url.searchParams.set('analyticsCountry', analyticsCountry.value);
      else url.searchParams.delete('analyticsCountry');
      if (analyticsType.value) url.searchParams.set('analyticsType', analyticsType.value);
      else url.searchParams.delete('analyticsType');
      window.history.replaceState({}, '', url);
    } catch (error) {
      if (requestVersion === analyticsRequestVersion) console.error(error);
    } finally {
      if (requestVersion === analyticsRequestVersion)
        [analyticsYear, analyticsCountry, analyticsType, analyticsReset, countryToggle, typeToggle, yearToggle].forEach(control => { if (control) control.disabled = false; });
    }
  };
  [analyticsYear, analyticsCountry, analyticsType].forEach(control => control?.addEventListener('change', updateAnalytics));
  analyticsReset?.addEventListener('click', () => {
    analyticsCountry.value = '';
    countryToggle.querySelector('span').textContent = 'All Countries';
    countryOptions.querySelectorAll('[role="option"]').forEach(option => option.setAttribute('aria-selected', String(!option.dataset.value)));
    closeCountryOptions();
    analyticsType.value = '';
    typeToggle.querySelector('span').textContent = 'All Types';
    typeOptions.querySelectorAll('[role="option"]').forEach(option => option.setAttribute('aria-selected', String(!option.dataset.value)));
    closeTypeOptions();
    const currentYear = String(new Date().getFullYear());
    analyticsYear.value = [...yearOptions.querySelectorAll('[role="option"]')].some(option => option.dataset.value === currentYear) ? currentYear : '2026';
    yearToggle.querySelector('span').textContent = analyticsYear.value;
    yearOptions.querySelectorAll('[role="option"]').forEach(option => option.setAttribute('aria-selected', String(option.dataset.value === analyticsYear.value)));
    closeYearOptions();
    updateAnalytics();
  });

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
    controls.append(link('Previous', Math.max(1, current - 1), current === 1, false));
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
    } catch (error) {
      console.error(error);
    } finally {
      changedControl.disabled = false;
    }
  };
  document.querySelectorAll('.fmcs-one-header-select-form, .fmcs-one-filter-form').forEach(form => {
    form.addEventListener('submit', event => event.preventDefault());
    form.querySelectorAll('select').forEach(select => select.addEventListener('change', () => refreshTable(form, select)));
  });
});
