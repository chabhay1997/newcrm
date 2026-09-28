(function () {
  'use strict';

  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const groups = [
    {key:'submit', label:'Submit', icon:'▣', color:'#2da5ef'},
    {key:'approve', label:'Approve', icon:'✓', color:'#6b4ae3'},
    {key:'query', label:'Query', icon:'▤', color:'#09c77b'},
    {key:'grant', label:'Grant', icon:'✹', color:'#ff732b'},
    {key:'total', label:'Total', icon:'▥', color:'#718bca'}
  ];

  document.addEventListener('DOMContentLoaded', function () {
    const finishingStyles = document.createElement('link');
    finishingStyles.rel = 'stylesheet';
    finishingStyles.href = '/css/trademark-analytics-overrides.css';
    document.head.appendChild(finishingStyles);
    const trigger = document.querySelector('.graph');
    if (!trigger) return;
    trigger.type = 'button';
    const ui = build();
    document.body.appendChild(ui.overlay);

    trigger.addEventListener('click', function (event) {
      event.preventDefault();
      ui.overlay.hidden = false;
      document.body.classList.add('tm-analytics-open');
      ui.close.focus();
      if (!ui.loaded) load(ui, true);
    });
    ui.close.addEventListener('click', function () { close(ui); });
    ui.overlay.addEventListener('click', function (event) { if (event.target === ui.overlay) close(ui); });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && !ui.overlay.hidden) close(ui);
    });
    ui.year.addEventListener('change', function () { load(ui); });
    ui.month.addEventListener('change', function () { load(ui); });
    ui.chartButtons.forEach(function (button) {
      button.addEventListener('click', function () {
        ui.chartMode = button.dataset.chartMode;
        ui.chartButtons.forEach(item => item.classList.toggle('active', item === button));
        if (ui.data) drawChart(ui, ui.data.monthly || []);
      });
    });
    ui.clear.addEventListener('click', function () {
      ui.month.value = '0';
      if (ui.defaultYear) ui.year.value = String(ui.defaultYear);
      load(ui);
    });
  });

  function build() {
    const overlay = document.createElement('div');
    overlay.className = 'tm-analytics-overlay';
    overlay.hidden = true;
    overlay.innerHTML = `<section class="tm-analytics-modal" role="dialog" aria-modal="true" aria-labelledby="tm-analytics-title">
      <header class="tm-analytics-head">
        <div class="tm-title tm-analytics-logo-wrap"><span class="tm-shield" aria-hidden="true"><svg viewBox="0 0 48 48"><defs><linearGradient id="tm-analytics-logo-gradient" x1="8" y1="6" x2="40" y2="43" gradientUnits="userSpaceOnUse"><stop stop-color="#3987ff"/><stop offset="1" stop-color="#0758d4"/></linearGradient></defs><path class="tm-analytics-logo-shield" d="M24 4.5 40 10v11.7c0 10.4-6.6 17.8-16 21.8-9.4-4-16-11.4-16-21.8V10l16-5.5Z"/><path class="tm-logo-highlight" d="M24 8.5 36 12.6v8.8c0 7.9-4.6 13.8-12 17.4-7.4-3.6-12-9.5-12-17.4v-8.8L24 8.5Z"/><text x="24" y="27" text-anchor="middle">TM</text></svg></span></div>
        <div><h2 id="tm-analytics-title">Trademark Statistics</h2></div>
        <button type="button" class="tm-analytics-close" aria-label="Close">&times;</button>
      </header>
      <div class="tm-analytics-filters">
        <label>Select Year<select class="tm-analytics-year" aria-label="Select year"></select></label>
        <label>Select Month<select class="tm-analytics-month" aria-label="Select month"><option value="0">All Months</option>${months.map((m,i)=>`<option value="${i+1}">${m}</option>`).join('')}</select></label>
        <button type="button" class="tm-analytics-clear">↻&nbsp;&nbsp; Clear</button>
      </div>
      <div class="tm-analytics-cards">${groups.map(g=>`<article class="tm-analytics-card ${g.key}"><header><i>${g.icon}</i><div><span>${g.label}</span><strong data-count="${g.key}">0</strong></div></header><footer data-trend="${g.key}">— &nbsp;No records</footer></article>`).join('')}</div>
      <div class="tm-analytics-content">
        <article class="tm-chart-panel">
          <div class="tm-panel-title"><h3>Trademark Stats (<span data-chart-year>—</span>)</h3><div class="tm-chart-toggle" role="group" aria-label="Chart type"><button class="active" type="button" data-chart-mode="bar" aria-label="Show bar chart" title="Bar chart">▥</button><button type="button" data-chart-mode="line" aria-label="Show line chart" title="Line chart">⌁</button></div></div>
          <div class="tm-bar-chart" data-chart></div>
          <div class="tm-chart-legend">${groups.map(g=>`<span style="--legend:${g.color}">${g.label}</span>`).join('')}</div>
        </article>
        <aside class="tm-summary-panel">
          <h3>Total Summary (<span data-summary-year>—</span>)</h3>
          <div class="tm-donut" data-donut><div class="tm-donut-center"><strong data-donut-total>0</strong><small>Total</small></div></div>
          <div class="tm-summary-list" data-summary></div>
        </aside>
      </div>
      <div class="tm-analytics-loading" hidden>Loading analytics…</div>
    </section>`;
    return {overlay:overlay, close:overlay.querySelector('.tm-analytics-close'), year:overlay.querySelector('.tm-analytics-year'), month:overlay.querySelector('.tm-analytics-month'), clear:overlay.querySelector('.tm-analytics-clear'), loading:overlay.querySelector('.tm-analytics-loading'), chartButtons:Array.from(overlay.querySelectorAll('[data-chart-mode]')), chartMode:'bar', data:null, defaultYear:null, loaded:false};
  }

  function close(ui) {
    ui.overlay.hidden = true;
    document.body.classList.remove('tm-analytics-open');
  }

  async function load(ui, first) {
    ui.loading.hidden = false;
    try {
      const params = new URLSearchParams();
      if (!first && ui.year.value) params.set('year', ui.year.value);
      if (!first && ui.month.value !== '0') params.set('month', ui.month.value);
      const response = await fetch('/operation/trademark/analytics' + (params.toString() ? '?' + params : ''), {headers:{Accept:'application/json'}});
      if (!response.ok) throw new Error('Unable to load trademark analytics.');
      const data = await response.json();
      const years = data.years && data.years.length ? data.years : [data.year];
      ui.year.innerHTML = years.map(y=>`<option value="${y}">${y}</option>`).join('');
      ui.year.value = String(data.year);
      ui.month.value = String(data.month || 0);
      if (ui.defaultYear === null) ui.defaultYear = data.year;
      render(ui, data);
      ui.loaded = true;
      ui.loading.hidden = true;
    } catch (error) {
      ui.loading.textContent = error.message || 'Unable to load trademark analytics.';
    }
  }

  function render(ui, data) {
    ui.data = data;
    groups.forEach(function (group) {
      const value = Number(data[group.key]) || 0;
      ui.overlay.querySelector(`[data-count="${group.key}"]`).textContent = value.toLocaleString();
      ui.overlay.querySelector(`[data-trend="${group.key}"]`).innerHTML = value ? '<b>↑</b>&nbsp;&nbsp;Current selection' : '— &nbsp;No records';
    });
    ui.overlay.querySelector('[data-chart-year]').textContent = data.year;
    ui.overlay.querySelector('[data-summary-year]').textContent = data.year;
    drawChart(ui, data.monthly || []);
    drawDonut(ui, data);
  }

  function drawChart(ui, monthly) {
    if (ui.chartMode === 'line') drawLines(ui, monthly);
    else drawBars(ui, monthly);
  }

  function drawBars(ui, monthly) {
    const chart = ui.overlay.querySelector('[data-chart]');
    chart.classList.remove('tm-line-chart');
    const rows = months.map(function (label, index) {
      const value = monthly[index] || {};
      return {label:label, submit:+value.submit||0, approve:+value.approve||0, query:+value.query||0, grant:+value.grant||0};
    });
    const max = Math.max(1, ...rows.map(r=>r.submit+r.approve+r.query+r.grant));
    chart.innerHTML = rows.map(function (row) {
      const total = row.submit + row.approve + row.query + row.grant;
      const segments = ['submit','approve','query','grant'].map(function (key) {
        const value = row[key];
        return `<span class="tm-segment ${key}" style="height:${total ? value/total*100 : 0}%" title="${key}: ${value}"></span>`;
      }).join('');
      return `<div class="tm-month-bar"><span class="tm-bar-value">${total}</span><div class="tm-bar-stack" style="height:${total ? Math.max(3,total/max*88) : 2}%">${segments}</div><span class="tm-month-label">${row.label}</span></div>`;
    }).join('');
  }

  function drawLines(ui, monthly) {
    const chart = ui.overlay.querySelector('[data-chart]');
    chart.classList.add('tm-line-chart');
    const rows = months.map(function (label, index) {
      const value = monthly[index] || {};
      return {label:label, submit:+value.submit||0, approve:+value.approve||0, query:+value.query||0, grant:+value.grant||0};
    });
    const max = Math.max(1, ...rows.flatMap(row => ['submit','approve','query','grant'].map(key => row[key])));
    const left = 52, right = 978, top = 24, bottom = 252;
    const x = index => left + (right - left) * index / 11;
    const y = value => bottom - (bottom - top) * value / max;
    const ticks = Array.from({length:5}, (_, index) => Math.round(max * index / 4));
    const grid = ticks.map(value => `<g><line x1="${left}" y1="${y(value)}" x2="${right}" y2="${y(value)}"/><text x="42" y="${y(value)+4}" text-anchor="end">${value}</text></g>`).join('');
    const labels = rows.map((row,index) => `<text class="tm-line-month" x="${x(index)}" y="282" text-anchor="middle">${row.label}</text>`).join('');
    const series = [
      {key:'submit', color:'#2da5ef'}, {key:'approve', color:'#6b4ae3'},
      {key:'query', color:'#09c77b'}, {key:'grant', color:'#ff732b'}
    ].map(function (item) {
      const points = rows.map((row,index) => ({x:x(index), y:y(row[item.key]), value:row[item.key]}));
      const dots = points.map(point => `<circle cx="${point.x}" cy="${point.y}" r="4"><title>${item.key}: ${point.value}</title></circle>`).join('');
      return `<g class="tm-line-series" style="--series:${item.color}"><path d="${smoothPath(points)}"/>${dots}</g>`;
    }).join('');
    chart.innerHTML = `<svg viewBox="0 0 1000 295" role="img" aria-label="Monthly trademark line chart"><g class="tm-line-grid">${grid}</g>${series}${labels}</svg>`;
  }

  function smoothPath(points) {
    if (!points.length) return '';
    let path = `M ${points[0].x} ${points[0].y}`;
    for (let index = 0; index < points.length - 1; index++) {
      const current = points[index], next = points[index + 1];
      const middle = (current.x + next.x) / 2;
      path += ` C ${middle} ${current.y}, ${middle} ${next.y}, ${next.x} ${next.y}`;
    }
    return path;
  }

  function drawDonut(ui, data) {
    const total = +data.total || 0, submit = +data.submit || 0, approve = +data.approve || 0, query = +data.query || 0;
    const pct = value => total ? value / total * 100 : 0;
    const donut = ui.overlay.querySelector('[data-donut]');
    donut.style.setProperty('--submit-end', pct(submit)+'%');
    donut.style.setProperty('--approve-end', (pct(submit)+pct(approve))+'%');
    donut.style.setProperty('--query-end', (pct(submit)+pct(approve)+pct(query))+'%');
    donut.classList.toggle('empty', total === 0);
    ui.overlay.querySelector('[data-donut-total]').textContent = total.toLocaleString();
    ui.overlay.querySelector('[data-summary]').innerHTML = groups.map(function (group) {
      return `<div class="${group.key==='total'?'summary-total':''}"><span style="--legend:${group.color}">${group.label}</span><strong>${(+data[group.key]||0).toLocaleString()}</strong></div>`;
    }).join('');
  }
})();
