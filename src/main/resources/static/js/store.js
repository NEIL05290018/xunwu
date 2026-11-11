// Cookie 会话由服务器验证；前端不发送用户身份编号。
export let db;
export let currentUser=0;
export let account=null;
let csrf='';
async function request(path,method='GET',body){
  const headers={};
  if(method!=='GET') headers['X-CSRF-Token']=csrf;
  if(body!==undefined) headers['Content-Type']='application/json';
  let response;
  try {response=await fetch('/api'+path,{method,headers,credentials:'same-origin',body:body===undefined?undefined:JSON.stringify(body)});}
  catch {throw new Error('无法连接服务器，请检查服务并刷新核对状态；不要重复提交订单。');}
  const raw=await response.text();let data=null;
  try {data=raw?JSON.parse(raw):null;} catch {throw new Error(`服务器响应异常（${response.status}）`);}
  if(!response.ok){
    if(response.status===401&&!path.startsWith('/auth/')) {account=null;currentUser=0;db=null;window.dispatchEvent(new Event('auth-expired'));}
    throw new Error(data?.message||`请求失败（${response.status}）`);
  }
  return data;
}
export async function loadSession(){account=await request('/auth/session');csrf=account.csrf;currentUser=account.user_id||0;}
export async function login(values,register=false){await request(register?'/auth/register':'/auth/login','POST',values);await initialize();}
export async function logout(){await request('/auth/logout','POST');db=null;await loadSession();}
export async function refresh(){if(account?.role)db=await request('/market');}
export async function initialize(){await loadSession();await refresh();}
async function change(path,method,body,admin=false){
  const result=await request(path,method,body,admin);
  try{await refresh();}catch{throw new Error('操作已完成，但页面刷新失败。请刷新浏览器核对结果。');}
  return result;
}
export function user(id=currentUser){return db.users.find(x=>x.user_id===id);}
export function product(id){return db.products.find(x=>x.product_id===id);}
export function images(id){return db.product_images.filter(x=>x.product_id===id).map(x=>x.image_url);}
export function image(id){return images(id)[0]||'/assets/bag.svg';}
export function liked(id){return db.favorites.some(x=>x.user_id===currentUser&&x.product_id===id);}
export async function favorite(id){await change(`/products/${id}/favorite`,liked(id)?'DELETE':'PUT');}
export async function createOrder(id,method,address){return change('/orders','POST',{product_id:id,delivery_method:method,address_id:address||null});}
export async function transition(id,action,payload={}){return change(`/orders/${id}/actions/${action}`,'POST',payload);}
export async function requestRefund(id,reason){return change(`/orders/${id}/refunds`,'POST',{content:reason});}
export async function decideRefund(id,agree){return change(`/refunds/${id}`,'PUT',{agree,note:''});}
export async function conversation(peer){const result=await change('/conversations','POST',{peer_id:peer});return result.conversation_id;}
export async function send(id,content){return change(`/conversations/${id}/messages`,'POST',{content});}
export async function publish(id,body){return change('/products'+(id?'/'+id:''),id?'PUT':'POST',body);}
export async function address(id,body){return change('/addresses'+(id?'/'+id:''),id?'PUT':'POST',body);}
export async function review(id,content){return change(`/orders/${id}/reviews`,'POST',{content});}
export async function report(id,content){return change(`/products/${id}/reports`,'POST',{content});}
export async function audit(id,agree,note){return change(`/admin/products/${id}/audit`,'PUT',{agree,note},true);}
export async function decideReport(id,agree,note){return change(`/admin/reports/${id}`,'PUT',{agree,note},true);}
export async function deleteProduct(id){return change(`/products/${id}`,'DELETE');}
