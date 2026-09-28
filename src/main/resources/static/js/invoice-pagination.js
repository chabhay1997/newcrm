document.addEventListener('DOMContentLoaded',()=>{
 const summary=[...document.querySelectorAll('span')].find(el=>/^Showing\s+.*invoices$/i.test(el.textContent.trim()));
 if(!summary)return;
 summary.textContent=summary.textContent.replace(/invoices$/i,'entries');
 const footer=summary.parentElement,oldControls=footer.lastElementChild;
 const links=[...oldControls.querySelectorAll('a')],numbered=links.filter(a=>/^\d+$/.test(a.textContent.trim()));
 const current=Number(numbered.find(a=>a.getAttribute('style')?.includes('linear-gradient'))?.textContent||new URL(location.href).searchParams.get('page')||1);
 const total=Math.max(1,...numbered.map(a=>Number(a.textContent.trim())));
 const urlFor=page=>{let url=new URL(location.href);url.searchParams.set('page',String(page));return url.pathname+url.search};
 const pages=[];for(let p=Math.max(1,current-2);p<=Math.min(total,current+2);p++)pages.push(p);
 footer.className='classic-pagination';summary.className='';oldControls.className='classic-pagination-controls';
 oldControls.innerHTML=`<input type="number" min="1" max="${total}" placeholder="Go to..." aria-label="Go to page"><button type="button" class="go-button">Go</button><a class="${current===1?'disabled':''}" href="${urlFor(Math.max(1,current-1))}">Previous</a>${pages.map(p=>`<a href="${urlFor(p)}" ${p===current?'aria-current="page"':''}>${p}</a>`).join('')}<a class="${current===total?'disabled':''}" href="${urlFor(Math.min(total,current+1))}">Next</a>`;
 const input=oldControls.querySelector('input'),go=()=>{let page=Number(input.value);if(page>=1&&page<=total)location.href=urlFor(page)};oldControls.querySelector('.go-button').onclick=go;input.onkeydown=e=>{if(e.key==='Enter')go()};
});
