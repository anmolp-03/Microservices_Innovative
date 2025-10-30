// Simple SPA for the restaurant UI (vanilla JS)
const state = {
  menu: [],
};

function qs(sel) { return document.querySelector(sel) }
function qsa(sel) { return Array.from(document.querySelectorAll(sel)) }

async function fetchMenu(){
  const res = await fetch('/menu');
  if(!res.ok) throw new Error('Failed to load menu');
  const data = await res.json();
  state.menu = data;
  renderMenu();
  populateMenuSelect();
}

function renderMenu(){
  const el = qs('#menu');
  if(!state.menu || state.menu.length===0){ el.innerText = 'No items'; return }
  el.innerHTML = state.menu.map(m => `
    <div class="menu-item">
      <div class="mi-name">${m.name}</div>
      <div class="mi-price">$${m.price.toFixed(2)}</div>
      <div class="mi-desc">${m.description||''}</div>
    </div>
  `).join('\n');
}

function populateMenuSelect(){
  const sel = qs('#menuSelect');
  sel.innerHTML = '';
  state.menu.forEach(m => {
    const opt = document.createElement('option'); opt.value = m.id; opt.text = `${m.name} — $${m.price}`; sel.appendChild(opt);
  })
}

function switchView(name){
  qsa('nav button').forEach(b => b.classList.toggle('active', b.dataset.view===name));
  qsa('.view').forEach(v => v.style.display = (v.id===`view-${name}`) ? '' : 'none');
}

function loadLocal(key){
  try{ return JSON.parse(localStorage.getItem(key) || '[]') }catch(e){ return [] }
}
function saveLocal(key, val){ localStorage.setItem(key, JSON.stringify(val)) }

async function placeOrder(){
  const customer = qs('#customer').value || 'anon';
  const menu_id = Number(qs('#menuSelect').value);
  const qty = Number(qs('#qty').value) || 1;
  const payload = { customer, items: [{ menu_id, qty }] };
  const res = await fetch('/orders', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) });
  if(!res.ok){ qs('#orderOut').innerText = 'Order failed'; return }
  const data = await res.json();
  qs('#orderOut').innerText = JSON.stringify(data, null, 2);
  // store in local history
  const hist = loadLocal('orders');
  hist.unshift({ order_id: data.order_id, customer, items: payload.items, created: new Date().toISOString() });
  saveLocal('orders', hist.slice(0,50));
  renderHistory();
}

function renderHistory(){
  const hist = loadLocal('orders');
  const el = qs('#history');
  if(hist.length===0){ el.innerText = 'No orders yet.'; return }
  el.innerHTML = hist.map(h => `
    <div class="order-row">
      <div><strong>#${h.order_id}</strong> ${h.customer} — <small>${new Date(h.created).toLocaleString()}</small></div>
      <div>Items: ${h.items.map(i=>`(${i.menu_id} x${i.qty})`).join(', ')}</div>
      <div><button data-order="${h.order_id}" class="btn-bill">Compute Bill</button></div>
    </div>
  `).join('\n');
  // attach handlers
  qsa('.btn-bill').forEach(b => b.addEventListener('click', async ()=>{
    const id = b.dataset.order; await computeBill(id); renderBills();
  }))
}

async function computeBill(order_id){
  const res = await fetch('/bills', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ order_id: Number(order_id) }) });
  if(!res.ok) { alert('Bill request failed'); return }
  const data = await res.json();
  const bills = loadLocal('bills');
  bills.unshift(Object.assign({ created: new Date().toISOString(), order_id: Number(order_id) }, data));
  saveLocal('bills', bills.slice(0,50));
  alert(`Bill for order ${order_id}: $${data.total}`)
}

function renderBills(){
  const bills = loadLocal('bills');
  const el = qs('#bills');
  if(bills.length===0){ el.innerText = 'No bills yet.'; return }
  el.innerHTML = bills.map(b => `
    <div class="bill-row"><strong>Bill #${b.bill_id}</strong> Order:${b.order_id} Total:$${Number(b.total).toFixed(2)} <small>${new Date(b.created).toLocaleString()}</small></div>
  `).join('\n');
}

async function postReview(){
  const customer = qs('#revCustomer').value || 'anon';
  const rating = Number(qs('#revRating').value) || 5;
  const comment = qs('#revComment').value || '';
  const res = await fetch('/reviews', { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify({ customer, rating, comment }) });
  if(res.status !== 201 && res.status !== 200){ alert('Review failed'); return }
  const reviews = loadLocal('reviews');
  reviews.unshift({ customer, rating, comment, created: new Date().toISOString() }); saveLocal('reviews', reviews.slice(0,100));
  renderReviews();
}

function renderReviews(){
  const reviews = loadLocal('reviews');
  const el = qs('#reviews');
  if(reviews.length===0){ el.innerText = 'No reviews yet.'; return }
  el.innerHTML = reviews.map(r => `<div class="rev-row"><strong>${r.customer}</strong> (${r.rating}/5) <div>${r.comment}</div> <small>${new Date(r.created).toLocaleString()}</small></div>`).join('\n');
}

function attachEvents(){
  qsa('nav button').forEach(b => b.addEventListener('click', ()=> switchView(b.dataset.view)));
  qs('#orderBtn').addEventListener('click', placeOrder);
  qs('#billBtn').addEventListener('click', ()=> computeBill(qs('#billOrderId').value));
  qs('#revBtn').addEventListener('click', postReview);
}

async function init(){
  attachEvents();
  try{ await fetchMenu() }catch(e){ qs('#menu').innerText = 'Failed to load menu' }
  renderHistory(); renderBills(); renderReviews();
}

window.addEventListener('load', init);
