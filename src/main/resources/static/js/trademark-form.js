document.addEventListener('DOMContentLoaded', () => {
  const editMatch = location.pathname.match(/^\/operation\/trademark\/(\d+)\/edit\/?$/);
  if (editMatch) initialiseEditForm(editMatch[1]);

  const payment = document.querySelector('select[name="paymentStatus"]');
  if (payment && !payment.querySelector('option[value="2"]')) {
    const half = new Option('Half', '2');
    const no = payment.querySelector('option[value="0"]');
    payment.insertBefore(half, no);
  }
  if (payment && !payment.querySelector('option[value="3"]')) payment.appendChild(new Option('Pending', '3'));

  const status = document.querySelector('select[name="status"]');
  const queryReply = [...(status?.options || [])].find(option => option.textContent.trim() === 'Query Reply');
  if (queryReply) {
    queryReply.textContent = 'Query Replied';
    queryReply.value = 'Query Replied';
  }
});

async function initialiseEditForm(id) {
  const form = document.getElementById('trademarkCreateForm');
  if (!form) return;
  form.action = `/operation/trademark/${encodeURIComponent(id)}`;
  const heading = document.querySelector('.tm-form-head-actions strong');
  if (heading) heading.textContent = 'Edit Trademark';
  document.title = 'Edit Trademark | EVTL CRM';
  const submit = document.querySelector('.tm-submit[form="trademarkCreateForm"]');
  if (submit) {
    const text = [...submit.childNodes].find(node => node.nodeType === Node.TEXT_NODE);
    if (text) text.nodeValue = ' Update Trademark';
  }
  form.setAttribute('aria-busy', 'true');
  try {
    const response = await fetch(`/operation/trademark/${encodeURIComponent(id)}/data`, {headers:{Accept:'application/json'}});
    if (!response.ok) throw new Error('Unable to load trademark details.');
    const record = await response.json();
    const values = {
      companyName: record.company,
      date: record.date,
      contactNo: record.phone,
      mailId: record.email,
      tradeMarkClass: record.tradeClass,
      tradeMarkName: record.trade_mark_name,
      appNo: record.applicationNo,
      paymentStatus: record.paymentCode,
      markStatus: record.markStatus,
      status: record.status,
      validity: record.validity,
      address: record.address,
      remark: record.remarks
    };
    Object.entries(values).forEach(([name, value]) => {
      const field = form.elements[name];
      if (!field) return;
      field.value = !value || value === 'N/A' ? '' : value;
    });
  } catch (error) {
    const alert = document.createElement('div');
    alert.className = 'tm-form-error';
    alert.textContent = error.message;
    form.before(alert);
  } finally {
    form.removeAttribute('aria-busy');
  }
}
