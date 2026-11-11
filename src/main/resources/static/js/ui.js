export const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const money = cents => (cents / 100).toLocaleString('zh-CN', {maximumFractionDigits:2});
const paths={
 search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
 heart:'<path d="M20.8 5.6a5.4 5.4 0 0 0-7.6 0L12 6.8l-1.2-1.2a5.4 5.4 0 0 0-7.6 7.6L12 22l8.8-8.8a5.4 5.4 0 0 0 0-7.6Z"/>',
 grid:'<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
 monitor:'<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 22h8M12 17v5"/>',
 headphones:'<path d="M3 14V11a9 9 0 0 1 18 0v3"/><rect x="2" y="12" width="5" height="9" rx="2"/><rect x="17" y="12" width="5" height="9" rx="2"/>',
 book:'<path d="M12 5C8 2 5 2 2 3v17c3-1 6-1 10 2 4-3 7-3 10-2V3c-3-1-6-1-10 2Zm0 0v17"/>',
 bag:'<path d="M5 7h14l2 14H3L5 7Z"/><path d="M8 8V5a4 4 0 0 1 8 0v3"/>',
 leaf:'<path d="M4 20C0 7 10 2 22 2c0 13-5 21-16 18M4 20 17 7"/>',
 shield:'<path d="M12 2 3 6v6c0 6 9 10 9 10s9-4 9-10V6l-9-4Z"/><path d="m8 12 3 3 5-6"/>',
 chat:'<path d="M21 11a9 9 0 0 1-9 9H3l2-5A9 9 0 1 1 21 11Z"/><path d="M8 10h8M8 14h5"/>',
 arrow:'<path d="M4 12h16m-6-6 6 6-6 6"/>',
 box:'<path d="m3 7 9-5 9 5v11l-9 5-9-5V7Zm0 0 9 5 9-5M12 12v11M7 4l10 6"/>',
 check:'<path d="m4 12 5 5L20 6"/>',
 bike:'<circle cx="5" cy="17" r="4"/><circle cx="19" cy="17" r="4"/><path d="m5 17 5-10 5 10H5l11-8 3 8M8 7h5M15 3h3l-2 6"/>',
 home:'<path d="m2 11 10-9 10 9M5 9v13h14V9M9 22v-8h6v8"/>'
};
export function icon(name) {return `<svg class="icon" viewBox="0 0 24 24" aria-hidden="true">${paths[name]||paths.box}</svg>`;}
export function status(text){const cls=['通过','已完成','退款成功','正常','在售'].includes(text)?'good':['待付款','待审核','待处理','待交付','待收货'].includes(text)?'warn':['驳回','成立','停用','已拒绝'].includes(text)?'bad':'';return `<span class="status ${cls}">${escape(text)}</span>`;}
export function empty(text, link=''){return `<div class="empty">${icon('leaf')}${escape(text)}${link}</div>`;}
let timer;
export function toast(text){const el=document.querySelector('#toast');el.textContent=text;el.classList.add('show');clearTimeout(timer);timer=setTimeout(()=>el.classList.remove('show'),3200);}
export function modal(title,content){const el=document.querySelector('#modal');el.setAttribute('aria-labelledby','modal-title');document.querySelector('#modal-content').innerHTML=`<div class="modal-body"><div class="modal-top"><h2 id="modal-title">${escape(title)}</h2><button class="close" data-action="close" aria-label="关闭对话框">×</button></div>${content}</div>`;if(!el.open)el.showModal();return el;}
export function close(){document.querySelector('#modal').close();}
export function error(message){const host=document.querySelector('#modal-content');let box=host.querySelector('.error-box');if(!box){box=document.createElement('div');box.className='error-box';box.setAttribute('role','alert');host.querySelector('.modal-top').after(box);}box.textContent=message;}
