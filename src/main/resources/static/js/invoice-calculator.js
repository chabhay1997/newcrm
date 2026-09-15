(function () {
  'use strict';

  const moneySelector = '.row-amount, #netAmountInput, #finalAmountInput, #extraServicesAmountInput';
  let activeAmountField = null;
  let expression = '';
  let displayValue = 0;

  function byId(id) { return document.getElementById(id); }
  function numberFrom(value) {
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  function formatMoney(value) {
    return Number.isFinite(value) ? value.toFixed(2) : '0.00';
  }
  function compactNumber(value) {
    if (!Number.isFinite(value)) return 'Error';
    return Number.parseFloat(value.toFixed(10)).toString();
  }

  // Small arithmetic parser: supports +, -, ×, ÷, parentheses and postfix percentages.
  function calculate(source) {
    const tokens = (source.match(/\d*\.?\d+|[()+\-*/%]/g) || []);
    if (!tokens.length || tokens.join('') !== source.replace(/\s+/g, '')) throw new Error('Invalid expression');
    let cursor = 0;

    function primary() {
      let value;
      const token = tokens[cursor++];
      if (token === '(') {
        value = addition();
        if (tokens[cursor++] !== ')') throw new Error('Missing parenthesis');
      } else if (token === '-') {
        value = -primary();
      } else if (token === '+') {
        value = primary();
      } else {
        value = Number(token);
        if (!Number.isFinite(value)) throw new Error('Invalid number');
      }
      while (tokens[cursor] === '%') { cursor++; value /= 100; }
      return value;
    }
    function multiplication() {
      let value = primary();
      while (tokens[cursor] === '*' || tokens[cursor] === '/') {
        const operator = tokens[cursor++];
        const right = primary();
        if (operator === '/' && right === 0) throw new Error('Cannot divide by zero');
        value = operator === '*' ? value * right : value / right;
      }
      return value;
    }
    function addition() {
      let value = multiplication();
      while (tokens[cursor] === '+' || tokens[cursor] === '-') {
        const operator = tokens[cursor++];
        const right = multiplication();
        value = operator === '+' ? value + right : value - right;
      }
      return value;
    }

    const result = addition();
    if (cursor !== tokens.length || !Number.isFinite(result)) throw new Error('Invalid expression');
    return result;
  }

  function renderDisplay(message) {
    byId('calculatorExpression').textContent = message || (expression || 'Ready for calculation');
    byId('calculatorDisplay').textContent = compactNumber(displayValue);
  }
  function solveExpression() {
    if (!expression) return displayValue;
    const result = calculate(expression);
    const completed = expression.replace(/\*/g, '×').replace(/\//g, '÷') + ' =';
    displayValue = result;
    expression = compactNumber(result);
    renderDisplay(completed);
    return result;
  }
  function loadIntoDisplay(value, label) {
    displayValue = numberFrom(value);
    expression = compactNumber(displayValue);
    renderDisplay(label);
  }
  function setResult(id, value) { byId(id).textContent = formatMoney(value); }

  function writeToInvoice(value) {
    if (!activeAmountField || !document.body.contains(activeAmountField)) {
      activeAmountField = document.querySelector('.row-amount');
    }
    if (!activeAmountField) return;
    activeAmountField.value = formatMoney(numberFrom(value));
    activeAmountField.dispatchEvent(new Event('input', { bubbles: true }));
    activeAmountField.dispatchEvent(new Event('change', { bubbles: true }));
    activeAmountField.classList.add('calculator-value-applied');
    window.setTimeout(() => activeAmountField && activeAmountField.classList.remove('calculator-value-applied'), 650);
  }

  function updateTax() {
    const base = numberFrom(byId('taxBaseAmount').value);
    const rate = numberFrom(byId('taxRate').value);
    const tax = base * rate / 100;
    setResult('taxAmountResult', tax);
    setResult('taxTotalResult', base + tax);
  }
  function updateDiscount() {
    const amount = numberFrom(byId('discountAmount').value);
    const rate = numberFrom(byId('discountRate').value);
    const discount = amount * rate / 100;
    setResult('discountResult', discount);
    setResult('discountNetResult', amount - discount);
  }
  function updateQuantity() {
    setResult('lineTotalResult', numberFrom(byId('quantityValue').value) * numberFrom(byId('rateValue').value));
  }
  function updateSplit() {
    const amount = numberFrom(byId('splitAmount').value);
    const count = Math.trunc(numberFrom(byId('splitCount').value));
    setResult('splitResult', count > 0 ? amount / count : 0);
  }

  function openCalculator() {
    const modal = byId('invoiceCalculatorModal');
    if (!activeAmountField) activeAmountField = document.querySelector('.row-amount');
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    byId('closeInvoiceCalculator').focus();
  }
  function closeCalculator() {
    const modal = byId('invoiceCalculatorModal');
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    byId('openInvoiceCalculator').focus();
  }

  function keypadAction(button) {
    const action = button.dataset.action;
    const value = button.dataset.value;
    if (value) {
      expression += value;
      try { displayValue = calculate(expression); } catch (ignored) { /* incomplete expressions are valid while typing */ }
      renderDisplay();
      return;
    }
    if (action === 'clear') { expression = ''; displayValue = 0; renderDisplay(); }
    if (action === 'delete') { expression = expression.slice(0, -1); displayValue = expression ? displayValue : 0; renderDisplay(); }
    if (action === 'negate') { expression = expression ? '-(' + expression + ')' : '-0'; try { displayValue = calculate(expression); } catch (ignored) {} renderDisplay(); }
    if (action === 'load-net') loadIntoDisplay(byId('netAmountInput').value, 'Net amount loaded');
    if (action === 'use-display') { try { writeToInvoice(solveExpression()); } catch (error) { renderDisplay(error.message); } }
    if (action === 'equals') { try { solveExpression(); } catch (error) { displayValue = 0; renderDisplay(error.message); } }
  }

  document.addEventListener('DOMContentLoaded', function () {
    const modal = byId('invoiceCalculatorModal');
    byId('openInvoiceCalculator').addEventListener('click', openCalculator);
    byId('closeInvoiceCalculator').addEventListener('click', closeCalculator);

    document.addEventListener('focusin', function (event) {
      if (event.target.matches(moneySelector)) activeAmountField = event.target;
    });
    modal.addEventListener('mousedown', function (event) {
      if (event.target === modal) closeCalculator();
    });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && modal.classList.contains('open')) closeCalculator();
    });

    byId('calculatorKeypad').addEventListener('click', function (event) {
      const button = event.target.closest('button');
      if (button) keypadAction(button);
    });
    modal.querySelectorAll('[data-action="use-display"]').forEach(function (button) {
      if (!button.closest('#calculatorKeypad')) button.addEventListener('click', function () {
        try { writeToInvoice(solveExpression()); } catch (error) { renderDisplay(error.message); }
      });
    });
    modal.querySelectorAll('[data-use]').forEach(function (button) {
      button.addEventListener('click', function () { writeToInvoice(byId(button.dataset.use).textContent); });
    });
    modal.querySelectorAll('[data-load-field]').forEach(function (button) {
      button.addEventListener('click', function () {
        const field = byId(button.dataset.loadField);
        loadIntoDisplay(field ? field.value : 0, button.textContent.trim().replace('Load', '').trim() + ' loaded');
      });
    });

    ['taxBaseAmount', 'taxRate'].forEach(id => byId(id).addEventListener('input', updateTax));
    ['discountAmount', 'discountRate'].forEach(id => byId(id).addEventListener('input', updateDiscount));
    ['quantityValue', 'rateValue'].forEach(id => byId(id).addEventListener('input', updateQuantity));
    ['splitAmount', 'splitCount'].forEach(id => byId(id).addEventListener('input', updateSplit));
    updateTax(); updateDiscount(); updateQuantity(); updateSplit(); renderDisplay();
  });
})();
