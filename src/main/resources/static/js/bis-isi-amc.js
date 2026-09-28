document.addEventListener('DOMContentLoaded', () => {
  const csrf = document.getElementById('amc-csrf');
  const fields = ['proposalDate', 'kindAttention', 'isStandard', 'product', 'cmlNumber', 'licenceValidityDate',
    'actualMarkingFee', 'sampleTestingFee', 'engineerVisitCharge', 'consultancyServiceFee', 'consultancyOneYear'];
  let backdrop;

  const notice = message => {
    const item = document.createElement('div');
    item.className = 'amc-client-notice';
    item.setAttribute('role', 'alert');
    item.textContent = message;
    document.body.appendChild(item);
    setTimeout(() => item.remove(), 6000);
  };
  const close = () => { backdrop?.remove(); backdrop = null; document.body.classList.remove('amc-modal-open'); };

  async function openQuotation(operationId) {
    let details;
    try {
      const response = await fetch(`/operation/bis-isi-amc/${operationId}/quotation`, {headers: {Accept: 'application/json'}});
      if (!response.ok) throw new Error(response.status === 403 ? 'You cannot edit this AMC operation.' : 'Unable to load the quotation form.');
      details = await response.json();
    } catch (error) { notice(error.message); return; }

    close();
    backdrop = document.createElement('div');
    backdrop.className = 'amc-modal-backdrop';
    backdrop.innerHTML = `<section class="amc-modal" role="dialog" aria-modal="true" aria-labelledby="amc-quote-title">
      <header class="amc-modal-head"><h2 id="amc-quote-title">AMC / SIT Quotation</h2><button type="button" class="amc-modal-close" aria-label="Close quotation form">&times;</button></header>
      <form class="amc-quote-form" method="post">
        <div class="amc-modal-scroll">
          <div class="amc-section-heading"><h3>Proposal Details</h3><div class="amc-revision-actions"><div class="amc-revisions" aria-label="Saved quotation revisions"></div><button type="button" class="amc-new">+ New</button></div></div>
          <div class="amc-form-grid three">
            <label>Ref. No.<input name="referenceNumber" readonly placeholder="Auto-generated on save"></label>
            <label>Date<input name="proposalDate" type="date" required></label>
            <label>Kind Attention<input name="kindAttention" maxlength="255"></label>
          </div>
          <div class="amc-form-grid two">
            <label>IS Standard<input name="isStandard" maxlength="255"></label>
            <label>Product<input name="product" maxlength="255"></label>
            <label>CML No.<input name="cmlNumber" maxlength="255"></label>
            <label>License Validity Date<input name="licenceValidityDate" type="date"></label>
          </div>
          <div class="amc-section-heading fee"><h3>BIS Fee Structure of SIT/AMC</h3></div>
          <div class="amc-form-grid two">
            <label>Actual Marking Fee (Unit Rate)<input name="actualMarkingFee" maxlength="255" placeholder="As per actual"></label>
            <label>Sample Testing Fee (6 Months)<input name="sampleTestingFee" maxlength="255" placeholder="As per actual"></label>
          </div>
          <div class="amc-form-grid three">
            <label>Engineer Visit Charge<input name="engineerVisitCharge" maxlength="255" placeholder="As per actual"></label>
            <label>Consultancy / Services Fee<input name="consultancyServiceFee" maxlength="255" placeholder="INR"></label>
            <label>Consultancy Charges for 1 Year<input name="consultancyOneYear" maxlength="255" placeholder="INR"></label>
          </div>
        </div>
        <footer class="amc-modal-footer"><button type="button" class="amc-cancel">Cancel</button><button type="submit" class="amc-save">Save Quotation</button></footer>
      </form>
    </section>`;
    document.body.appendChild(backdrop);
    document.body.classList.add('amc-modal-open');
    backdrop.querySelector('.amc-modal-close').addEventListener('click', close);
    backdrop.querySelector('.amc-cancel').addEventListener('click', close);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) close(); });
    const form = backdrop.querySelector('form');
    form.action = `/operation/bis-isi-amc/${operationId}/quotation`;
    const revisionInput = document.createElement('input');
    revisionInput.type = 'hidden'; revisionInput.name = 'revisionNumber'; form.appendChild(revisionInput);
    const newRevisionInput = document.createElement('input');
    newRevisionInput.type = 'hidden'; newRevisionInput.name = 'newRevision'; form.appendChild(newRevisionInput);
    if (csrf) { const token = csrf.cloneNode(); token.removeAttribute('id'); form.appendChild(token); }
    const url = new URL(location.href);
    [['returnSearch', url.searchParams.get('search') || ''], ['returnSize', url.searchParams.get('size') || '25'],
      ['returnPage', url.searchParams.get('page') || '0']].forEach(([name, value]) => {
      const hidden = document.createElement('input'); hidden.type = 'hidden'; hidden.name = name; hidden.value = value; form.appendChild(hidden);
    });
    const showDetails = data => {
      details = data;
      revisionInput.value = data.revisionNumber;
      newRevisionInput.value = String(!data.editing);
      form.elements.referenceNumber.value = data.referenceNumber || '';
      fields.forEach(name => { form.elements[name].value = data[name] || ''; });
      const revisions = backdrop.querySelector('.amc-revisions');
      revisions.replaceChildren();
      (data.revisions || []).forEach(number => {
        const button = document.createElement('button');
        button.type = 'button'; button.className = 'amc-revision';
        button.textContent = `R${number}`;
        button.setAttribute('aria-pressed', String(number === data.revisionNumber));
        if (number === data.revisionNumber) button.classList.add('selected');
        if (number === data.activeRevision) {
          button.classList.add('active-download');
          button.title = 'Currently selected for download';
        }
        button.addEventListener('click', () => loadRevision(number));
        revisions.appendChild(button);
      });
      backdrop.querySelector('.amc-new').textContent = `+ New`;
    };
    const loadRevision = async number => {
      if (Number(revisionInput.value) === number) return;
      try {
        const response = await fetch(`/operation/bis-isi-amc/${operationId}/quotation?revision=${number}`, {headers: {Accept: 'application/json'}});
        if (!response.ok) throw new Error('Unable to load this quotation revision.');
        showDetails(await response.json());
      } catch (error) { notice(error.message); }
    };
    showDetails(details);
    const newButton = backdrop.querySelector('.amc-new');
    newButton.addEventListener('click', () => loadRevision(details.nextRevision));
    form.addEventListener('submit', () => { const save = form.querySelector('.amc-save'); save.disabled = true; save.textContent = 'Saving…'; });
    form.elements.kindAttention.focus();
  }

  document.querySelectorAll('.amc-create[data-operation-id]').forEach(button =>
    button.addEventListener('click', () => openQuotation(button.dataset.operationId)));
  const searchForm = document.querySelector('.amc-search');
  const searchInput = document.getElementById('amc-search');
  const suggestions = document.getElementById('amc-search-suggestions');
  if (searchForm && searchInput && suggestions) {
    let timer, clearTimer, request, active = -1;
    const closeSuggestions = () => {
      suggestions.hidden = true;
      searchInput.setAttribute('aria-expanded', 'false');
      searchInput.removeAttribute('aria-activedescendant');
      active = -1;
    };
    const activate = index => {
      const options = [...suggestions.querySelectorAll('[role="option"]')];
      active = index;
      options.forEach((option, position) => option.classList.toggle('active', position === index));
      if (index >= 0) searchInput.setAttribute('aria-activedescendant', options[index].id);
      else searchInput.removeAttribute('aria-activedescendant');
    };
    searchInput.addEventListener('input', () => {
      clearTimeout(timer); clearTimeout(clearTimer); request?.abort(); closeSuggestions();
      const query = searchInput.value.trim();
      if (!query) {
        if (new URLSearchParams(location.search).has('search')) clearTimer = setTimeout(() => {
          if (searchInput.value.trim()) return;
          const url = new URL(location.href);
          url.searchParams.delete('search'); url.searchParams.delete('page');
          location.href = url.pathname + url.search;
        }, 250);
        return;
      }
      timer = setTimeout(async () => {
        request = new AbortController();
        try {
          const response = await fetch(`/operation/bis-isi-amc/suggestions?query=${encodeURIComponent(query)}`,
            {signal: request.signal, headers: {Accept: 'application/json'}});
          if (!response.ok) throw new Error('Suggestions unavailable');
          const results = await response.json();
          if (searchInput.value.trim() !== query) return;
          suggestions.replaceChildren();
          results.forEach((result, index) => {
            const option = document.createElement('div');
            option.id = `amc-search-suggestion-${index}`;
            option.className = 'amc-search-suggestion';
            option.setAttribute('role', 'option');
            const value = document.createElement('strong'); value.textContent = result.value;
            const detail = document.createElement('small'); detail.textContent = result.detail;
            option.append(value, detail);
            option.addEventListener('mousedown', event => event.preventDefault());
            option.addEventListener('click', () => { searchInput.value = result.value; closeSuggestions(); searchForm.requestSubmit(); });
            suggestions.appendChild(option);
          });
          suggestions.hidden = !results.length;
          searchInput.setAttribute('aria-expanded', String(!!results.length));
        } catch (error) { if (error.name !== 'AbortError') closeSuggestions(); }
      }, 180);
    });
    searchInput.addEventListener('keydown', event => {
      const options = [...suggestions.querySelectorAll('[role="option"]')];
      if (event.key === 'Escape') { closeSuggestions(); return; }
      if (suggestions.hidden || !options.length) return;
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        activate(event.key === 'ArrowDown' ? (active + 1) % options.length : (active - 1 + options.length) % options.length);
      } else if (event.key === 'Enter' && active >= 0) {
        event.preventDefault(); searchInput.value = options[active].querySelector('strong').textContent;
        closeSuggestions(); searchForm.requestSubmit();
      }
    });
    document.addEventListener('click', event => { if (!searchForm.contains(event.target)) closeSuggestions(); });
  }
  const pager = document.querySelector('.amc-footer.classic-pagination');
  if (pager) {
    const current = Number(pager.dataset.current), totalPages = Number(pager.dataset.pages);
    const total = Number(pager.dataset.total), size = Number(pager.dataset.size);
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
    controls.className = 'classic-pagination-controls'; controls.setAttribute('aria-label', 'AMC pages');
    const input = document.createElement('input');
    input.type = 'number'; input.min = '1'; input.max = String(totalPages);
    input.placeholder = 'Go to...'; input.setAttribute('aria-label', 'Go to page');
    const go = () => {
      const page = Number(input.value);
      if (Number.isInteger(page) && page >= 1 && page <= totalPages) location.href = urlFor(page);
      else input.setCustomValidity(`Enter a page from 1 to ${totalPages}.`);
    };
    input.addEventListener('input', () => input.setCustomValidity(''));
    input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); go(); } });
    const goButton = document.createElement('button');
    goButton.type = 'button'; goButton.className = 'go-button'; goButton.textContent = 'Go';
    goButton.addEventListener('click', go);
    const link = (label, page, disabled = false, selected = false) => {
      const item = document.createElement('a');
      item.textContent = label; item.href = urlFor(page);
      if (disabled) { item.className = 'disabled'; item.setAttribute('aria-disabled', 'true'); item.tabIndex = -1; }
      if (selected) item.setAttribute('aria-current', 'page');
      return item;
    };
    controls.append(input, goButton, link('Previous', Math.max(1, current - 1), current === 1));
    pages.forEach(page => controls.appendChild(link(String(page), page, false, page === current)));
    controls.appendChild(link('Next', Math.min(totalPages, current + 1), current === totalPages));
    pager.replaceChildren(count, controls);
  }
  document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
});
