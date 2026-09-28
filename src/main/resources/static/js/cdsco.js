document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.contact span:last-child').forEach(line => {
    line.querySelector('svg')?.remove();
    if (line.querySelector('b')?.textContent.trim().toUpperCase() === 'N/A') line.remove();
  });
  document.querySelectorAll('.cdsco-alert').forEach(alert => window.setTimeout(() => alert.remove(), 6000));

  const form = document.querySelector('.cdsco-filters');
  const search = form?.elements.search;
  const importFile = document.getElementById('cdsco-import-file');
  importFile?.addEventListener('change', () => {
    if (!importFile.files?.length) return;
    const upload = document.createElement('form');
    upload.method = 'post'; upload.action = '/operation/cdsco/import'; upload.enctype = 'multipart/form-data'; upload.hidden = true;
    importFile.name = 'file';
    const csrf = document.createElement('input'); csrf.type = 'hidden'; csrf.name = '_csrf'; csrf.value = document.querySelector('meta[name="_csrf"]')?.content || '';
    upload.append(importFile, csrf); document.body.appendChild(upload); upload.submit();
  });
  document.querySelector('.cdsco-excel-button.export')?.addEventListener('click', event => {
    event.preventDefault();
    const url = new URL('/operation/cdsco/export', location.origin);
    ['search','startDate','endDate','status'].forEach(name => {
      const value = form?.elements[name]?.value?.trim();
      if (value) url.searchParams.set(name, value);
    });
    location.href = url.pathname + url.search;
  });
  let timer;
  search?.addEventListener('input', () => {
    window.clearTimeout(timer);
    timer = window.setTimeout(() => form.requestSubmit(), 350);
  });
  form?.querySelectorAll('input[type="date"],select[name="status"],select[name="size"]').forEach(field => {
    field.addEventListener('change', () => form.requestSubmit());
  });

  const pager = document.querySelector('.cdsco-footer.classic-pagination');
  if (pager) {
    const current = Number(pager.dataset.current), pages = Number(pager.dataset.pages), total = Number(pager.dataset.total), size = Number(pager.dataset.size);
    const urlFor = page => { const url = new URL(location.href); url.searchParams.set('page', page - 1); return url.pathname + url.search; };
    const controls = pager.querySelector('.classic-pagination-controls');
    const link = (label, page, disabled, selected) => { const item=document.createElement('a'); item.textContent=label; item.href=urlFor(page); if(disabled){item.className='disabled';item.setAttribute('aria-disabled','true');} if(selected)item.setAttribute('aria-current','page'); return item; };
    controls.append(link('Previous', Math.max(1,current-1), current===1, false));
    for(let page=Math.max(1,current-2);page<=Math.min(pages,current+2);page++) controls.append(link(String(page),page,false,page===current));
    controls.append(link('Next',Math.min(pages,current+1),current===pages,false));
    const first=total?(current-1)*size+1:0,last=total?Math.min(current*size,total):0;
    pager.querySelector(':scope > span').textContent=`Showing ${first} to ${last} of ${total} entries`;
  }

  document.addEventListener('click', event => {
    const trigger = event.target.closest('.cdsco-menu-trigger');
    document.querySelectorAll('.cdsco-action-menu.open').forEach(menu => { if (!trigger || menu !== trigger.closest('.cdsco-action-menu')) menu.classList.remove('open'); });
    if (trigger) {
      const menu = trigger.closest('.cdsco-action-menu');
      const open = menu.classList.toggle('open');
      trigger.setAttribute('aria-expanded', String(open));
    }
    const remove = event.target.closest('.cdsco-delete');
    if (remove && !window.confirm('Permanently delete this CDSCO record?')) event.preventDefault();
  });
});
