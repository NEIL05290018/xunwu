import * as S from './store.js';
import {escape as e,money,icon,status,empty,toast,modal,close,error} from './ui.js';

const app=document.querySelector('#app');
const categories=[['全部好物','ALL FINDS','grid'],['数码设备','DIGITAL','monitor'],['数码配件','ACCESSORIES','headphones'],['图书教材','BOOKS','book'],['生活用品','LIFESTYLE','home'],['更多分类','MORE','bag']];
let category='全部好物',query='',sort='default',pickup=false,selectedChat=null,orderTab='全部',orderRole='buy';
let uploadImages=[];
let minPrice='',maxPrice='',searchSequence=0,pendingDelete=[];
const title=(heading,desc,extra='')=>`<div class="page-title"><div><h1>${heading}</h1><p>${desc}</p></div>${extra}</div>`;
const field=(name,label,value='',type='text',extra='')=>`<div class="field ${extra}"><label for="f-${name}">${label}</label><input id="f-${name}" name="${name}" type="${type}" value="${e(value)}" required maxlength="${type==='email'?120:80}"></div>`;
const addressOptions=()=>S.db.addresses.filter(a=>a.user_id===S.currentUser).map(a=>`<option value="${a.address_id}">${e(a.recipient_name+' · '+a.detail_address)}</option>`).join('');
const findOrder=id=>S.db.orders.find(o=>o.order_id===Number(id));

function productCard(p){
 return `<article class="product-card"><button class="favorite ${S.liked(p.product_id)?'on':''}" data-action="favorite" data-id="${p.product_id}" aria-label="${S.liked(p.product_id)?'取消收藏':'收藏'}${e(p.title)}" aria-pressed="${S.liked(p.product_id)}">${icon('heart')}</button><button class="product-picture" data-action="detail" data-id="${p.product_id}" aria-label="查看${e(p.title)}"><img src="${e(S.image(p.product_id))}" alt="${e(p.title)}示意图" loading="lazy"><span class="delivery-tag">${p.support_pickup?'支持自提':'支持快递'}${p.product_status!=='在售'?' · '+e(p.product_status):''}</span></button><div class="product-info"><button class="product-title" data-action="detail" data-id="${p.product_id}">${e(p.title)}</button><p class="product-desc">${e(p.description)}</p><div class="row between"><span class="price"><small>¥</small>${money(p.price_cents)}</span><span class="small">${e(p.category)}</span></div><div class="product-meta"><span class="seller"><span class="mini-avatar">${e(S.user(p.seller_id)?.nickname.slice(0,1))}</span>${e(S.user(p.seller_id)?.nickname)}</span><span>${S.liked(p.product_id)?'已收藏':'发现好物'}</span></div></div></article>`;
}
async function discover(){
 const seq=++searchSequence,hash=location.hash;
 let products;
 try {
  const params={scope:'market',q:query};
  if(!['全部好物','更多分类'].includes(category))params.category=category;
  if(minPrice!=='')params.minPrice=Math.round(Number(minPrice)*100);
  if(maxPrice!=='')params.maxPrice=Math.round(Number(maxPrice)*100);
  products=await S.searchProducts(params);
 } catch(err){if(seq===searchSequence&&hash===location.hash)toast(err.message);return;}
 if(seq!==searchSequence||hash!==location.hash||!S.account?.role)return;
 if(category==='更多分类') products=products.filter(p=>['服饰箱包','运动户外'].includes(p.category));
 else if(category!=='全部好物') products=products.filter(p=>p.category===category);

 if(pickup)products=products.filter(p=>p.support_pickup);
 if(sort==='low')products.sort((a,b)=>a.price_cents-b.price_cents);
 if(sort==='high')products.sort((a,b)=>b.price_cents-a.price_cents);
 if(sort==='new')products.sort((a,b)=>b.product_id-a.product_id);
 app.innerHTML=`<section class="hero"><div class="hero-copy"><div class="eyebrow">A SECOND LIFE, A NEW STORY</div><h1>闲置有去处，<br>好物<em>再相遇。</em></h1><p>把用不到的，变成别人刚好需要的。<br>在循物，发现生活里的下一份小惊喜。</p><a class="hero-link" href="#finds">去发现好物 ${icon('arrow')}</a></div><div class="hero-art" aria-hidden="true"><span class="hero-stamp">GOOD FINDS<br>好物循环</span><img class="hero-object camera" src="/assets/camera.svg" alt=""><img class="hero-object lamp" src="/assets/lamp.svg" alt=""><img class="hero-object book" src="/assets/book.svg" alt=""><span class="hero-note">让每件好物，多一个故事 ↗</span></div></section><div class="trust-strip"><span>${icon('leaf')}闲置循环，物尽其用</span><span>${icon('chat')}直接沟通，认真交易</span><span>${icon('box')}快递自提，自由选择</span></div><section id="finds"><div class="section-heading"><div><h2>发现你的下一件好物</h2><p>有些惊喜，不一定要买全新的。</p></div><small>CURATED EVERYDAY FINDS</small></div><div class="category-row">${categories.map(([c,en,ic])=>`<button class="category ${category===c?'active':''}" data-action="category" data-value="${c}" aria-pressed="${category===c}">${icon(ic)}<span>${c}<small>${en}</small></span></button>`).join('')}</div><div class="toolbar"><form id="search-form" class="search">${icon('search')}<input aria-label="搜索闲置商品" name="query" value="${e(query)}" placeholder="搜索你想要的好物…" maxlength="80"><input class="price-filter" aria-label="最低价格（元）" name="minPrice" type="number" min="0" max="1000000" step="0.01" value="${e(minPrice)}" placeholder="最低价（元）"><input class="price-filter" aria-label="最高价格（元）" name="maxPrice" type="number" min="0" max="1000000" step="0.01" value="${e(maxPrice)}" placeholder="最高价（元）"><button type="submit">搜索</button></form><div class="filters"><span>共 ${products.length} 件好物</span><label class="check"><input id="pickup-filter" type="checkbox" ${pickup?'checked':''}>仅看支持自提</label><select id="sort-filter" aria-label="商品排序"><option value="default">综合推荐</option><option value="new">最新发布</option><option value="low">价格从低到高</option><option value="high">价格从高到低</option></select></div></div><div class="product-grid">${products.map(productCard).join('')||empty('暂时没有符合条件的好物。试试其他分类或关键词。')}</div><div class="count-line"><span>已展示全部 ${products.length} 件好物 · 商品图片为本地示意插画</span><a class="pill" href="#finds">回到好物顶部 ↑</a></div></section><section class="manifesto"><div><h3>每一次转手，都是新的开始。</h3><p>让不再需要的物品，遇见刚好需要它的人。</p></div>${icon('leaf')}</section>`;
 document.querySelector('#sort-filter').value=sort;
}
async function favorites(){
 const hash=location.hash,seq=++searchSequence;
 try{const products=await S.joinedFavorites();if(seq!==searchSequence||hash!==location.hash||!S.account?.role)return;
 app.innerHTML=title('我的收藏','喜欢的好物，先替你留个位置。')+`<div class="product-grid">${products.map(p=>`<div>${productCard(p)}<p class="small">收藏于 ${e(p.favorited_at)}</p></div>`).join('')||empty('还没有收藏，去发现喜欢的好物吧。')}</div>`;
 }catch(err){toast(err.message);}
}
async function showDetail(id){
 let p=S.product(Number(id));if(!p)return toast('商品不存在');
 if(S.account.role==='ADMIN'){modal('商品详情',`<h2>${e(p.title)}</h2><p>${e(p.description)}</p><p>¥${money(p.price_cents)} · ${e(p.category)}</p><div class="thumbnails">${S.images(p.product_id).map(src=>`<img src="${e(src)}" alt="商品图片">`).join('')}</div>`);return;}
 try{p=await S.joinedDetail(id);}catch(err){toast(err.message);return;}
 const own=p.seller_id===S.currentUser,available=p.product_status==='在售'&&p.audit_status==='通过';
 modal('好物详情',`<div class="detail"><div><img class="detail-photo" id="detail-photo" src="${e(S.image(p.product_id))}" alt="${e(p.title)}示意图"><div class="thumbnails">${S.images(p.product_id).map((src,i)=>`<button data-action="thumbnail" data-index="${i}" data-id="${id}" aria-label="查看第${i+1}张图片"><img src="${e(src)}" alt="缩略图${i+1}"></button>`).join('')}</div><p class="small">商品图片为示意插画；你发布时可上传真实图片。</p></div><div class="detail-info">${status(p.product_status)} ${status(p.audit_status)}<h2>${e(p.title)}</h2><span class="price"><small>¥</small>${money(p.price_cents)}</span><p>${e(p.description)}</p><div class="row"><span class="avatar" style="display:grid;place-items:center">${e(S.user(p.seller_id)?.nickname[0])}</span><div><b>${e(p.seller_nickname)}</b><div class="small">${e(p.category)} · ${p.support_express?'快递 ':''}${p.support_pickup?'自提':''}</div></div></div><div class="form-actions">${own?`<button class="primary" data-action="edit-product" data-id="${id}" ${p.product_status==='锁定'||p.product_status==='已售'?'disabled':''}>编辑我的商品</button><button class="secondary" data-action="delete-product" data-id="${id}" ${['锁定','已售'].includes(p.product_status)?'disabled':''}>删除商品</button>`:`<button class="primary" data-action="buy" data-id="${id}" ${available?'':'disabled'}>我想要</button><button class="secondary" data-action="contact" data-id="${p.seller_id}">联系卖家</button>`}<button class="secondary" data-action="detail-favorite" data-id="${id}">${S.liked(p.product_id)?'已收藏':'收藏好物'} ${icon('heart')}</button></div>${!own?`<button class="detail-report" data-action="report" data-id="${id}">商品有问题？提交举报</button>`:''}</div></div>`);
}
function buy(id){
 const p=S.product(Number(id));
 modal('确认订单',`<p class="small">${e(p.title)} · ¥${money(p.price_cents)}。当前为本地演示，不发生真实付款。</p><form id="buy-form" data-id="${id}"><div class="form-grid"><div class="field full"><label for="delivery">交付方式</label><select id="delivery" name="delivery_method">${p.support_pickup?'<option value="自提">自提 · 免运费，时间地点通过聊天协商</option>':''}${p.support_express?'<option value="快递">快递 · 演示运费 ¥6</option>':''}</select></div><div class="field full" id="address-field"><label for="address">快递收货地址</label><select id="address" name="address_id"><option value="">请选择收货地址</option>${addressOptions()}</select><span class="small">没有地址？请先到“我的”添加。自提不需要地址。</span></div></div><p id="buy-total" class="hint"></p><div class="form-actions"><button type="button" class="secondary" data-action="close">再想想</button><button class="primary">提交演示订单</button></div></form>`);
 updateDelivery();
}
function updateDelivery(){const select=document.querySelector('#delivery');if(!select)return;const express=select.value==='快递',id=Number(document.querySelector('#buy-form').dataset.id);document.querySelector('#address-field').hidden=!express;document.querySelector('#address').required=express;document.querySelector('#buy-total').textContent=`合计 ¥${money(S.product(id).price_cents+(express?600:0))}（含运费 ¥${express?6:0}）`;} 
function publish(id){
 const p=id?S.product(Number(id)):null;
 if(p&&(p.seller_id!==S.currentUser||['锁定','已售'].includes(p.product_status)))return toast('该商品当前无法编辑');
 uploadImages=p?S.images(p.product_id):[];
 modal(p?'编辑闲置':'发布一件闲置',`<p class="small">把成色、使用情况和瑕疵写进描述，方便买家了解。提交后进入待审核状态。</p><form id="publish-form" data-id="${p?.product_id||''}"><div class="form-grid">${field('title','商品标题',p?.title||'','text','full')}<div class="field full"><label for="description">商品描述</label><textarea id="description" name="description" required maxlength="1000" placeholder="讲讲它的使用情况，以及你想转手的原因…">${e(p?.description||'')}</textarea></div><div class="field"><label for="price">价格（元）</label><input id="price" name="price" type="number" min="0.01" max="1000000" step="0.01" required value="${p?p.price_cents/100:''}"></div><div class="field"><label for="category">商品分类</label><select id="category" name="category">${['数码设备','数码配件','图书教材','生活用品','服饰箱包','运动户外'].map(c=>`<option ${p?.category===c?'selected':''}>${c}</option>`).join('')}</select></div><div class="field full"><span>交付方式（至少一种）</span><div class="row"><label class="check"><input type="checkbox" name="support_pickup" ${!p||p.support_pickup?'checked':''}>支持自提</label><label class="check"><input type="checkbox" name="support_express" ${!p||p.support_express?'checked':''}>支持快递</label></div></div><div class="field full"><label for="photo-files">商品图片（最多 4 张，每张不超过 1MB）</label><div class="upload-box"><input id="photo-files" type="file" accept="image/png,image/jpeg,image/webp" multiple><p class="small">选择新图片会替换当前图片；不选则保留原图或使用默认示意图。</p><div id="upload-preview" class="upload-preview">${uploadImages.map(src=>`<img src="${e(src)}" alt="商品预览">`).join('')}</div></div></div></div><div class="form-actions"><button type="button" class="secondary" data-action="close">取消</button><button class="primary" id="publish-submit">提交审核</button></div></form>`);
}
function orders(){
 const all=S.db.orders.filter(o=>orderRole==='buy'?o.buyer_id===S.currentUser:S.product(o.product_id)?.seller_id===S.currentUser);
 const list=all.filter(o=>orderTab==='全部'||o.order_status===orderTab).sort((a,b)=>b.order_id-a.order_id);
 app.innerHTML=title('我的订单','每一笔交易，都有清楚的来处与去向。',`<button class="secondary" data-action="role">${orderRole==='buy'?'切换到我卖出的':'切换到我买到的'} ⇄</button>`)+`<div class="tabs">${['全部','待付款','待交付','待收货','已完成','已取消','已退款'].map(t=>`<button class="tab ${orderTab===t?'active':''}" data-action="order-tab" data-value="${t}">${t} ${all.filter(o=>t==='全部'||o.order_status===t).length}</button>`).join('')}</div><p class="small">当前查看：${orderRole==='buy'?'我买到的':'我卖出的'} · 操作通过 REST 接口更新MySQL 数据。</p><div class="panel">${list.map(orderRow).join('')||empty('这里还没有订单。','<a href="#discover">去发现好物 →</a>')}</div>`;
}
function orderRow(o){
 const p=S.product(o.product_id),buyer=o.buyer_id===S.currentUser,refund=S.db.refunds.find(r=>r.order_id===o.order_id),pending=refund?.refund_status==='待处理';
 let actions=`<button class="secondary" data-action="order-detail" data-id="${o.order_id}">订单详情</button>`;
 if(buyer&&o.order_status==='待付款')actions+=`<button class="secondary" data-action="cancel" data-id="${o.order_id}">取消</button><button class="primary" data-action="pay" data-id="${o.order_id}">模拟付款</button>`;
 if(!buyer&&o.order_status==='待交付'&&!pending)actions+=`<button class="primary" data-action="ship" data-id="${o.order_id}">${o.delivery_method==='自提'?'确认自提交付':'填写发货信息'}</button>`;
 if(buyer&&o.order_status==='待收货'&&!pending)actions+=`<button class="primary" data-action="receive" data-id="${o.order_id}">确认收货</button>`;
 if(buyer&&['待交付','待收货','已完成'].includes(o.order_status)&&!refund)actions+=`<button class="secondary" data-action="refund" data-id="${o.order_id}">申请退款</button>`;
 if(!buyer&&pending)actions+=`<button class="secondary" data-action="refund-reject" data-id="${refund.refund_id}">拒绝退款</button><button class="primary" data-action="refund-agree" data-id="${refund.refund_id}">同意退款</button>`;
 if(o.order_status==='已完成'&&!S.db.reviews.some(r=>r.order_id===o.order_id&&r.reviewer_id===S.currentUser))actions+=`<button class="secondary" data-action="review" data-id="${o.order_id}">写评价</button>`;
 return `<article class="order-row"><img src="${e(S.image(o.product_id))}" alt="${e(o.snapshot_title)}示意图"><div><div class="small">订单 #${o.order_id} · ${e(o.created_at.slice(0,10))}</div><h3>${e(o.snapshot_title)}</h3><div class="small">${buyer?'卖家':'买家'}：${e(S.user(buyer?p.seller_id:o.buyer_id)?.nickname)} · ${e(o.delivery_method)} ${refund?' · 退款：'+e(refund.refund_status):''}</div>${refund?`<div class="small">退款原因：${e(refund.reason)}</div>`:''}${status(o.order_status)}</div><div class="order-price"><span class="price"><small>¥</small>${money(o.deal_price_cents+o.shipping_fee_cents)}</span><div class="order-actions">${actions}</div></div></article>`;
}
function orderDetail(id){const o=findOrder(id);modal('订单 #'+id,`<div class="info-list"><p><b>商品快照</b>${e(o.snapshot_title)}</p><p><b>订单状态</b>${status(o.order_status)}</p><p><b>成交金额</b>¥${money(o.deal_price_cents)} + 运费 ¥${money(o.shipping_fee_cents)}</p><p><b>交付方式</b>${e(o.delivery_method)}</p><p><b>创建时间</b>${e(o.created_at)}</p>${o.delivery_method==='快递'?`<p><b>收货人</b>${e(o.recipient_name)} · ${e(o.recipient_phone)}</p><p><b>地址快照</b>${e(o.recipient_address)}</p><p><b>物流信息</b>${e(o.carrier||'尚未发货')} ${e(o.tracking_no||'')}</p>`:'<p>自提时间和地点由双方通过聊天协商。</p>'}</div><h3>双方评价</h3>${S.db.reviews.filter(r=>r.order_id===o.order_id).map(r=>`<div class="review"><b>${e(S.user(r.reviewer_id)?.nickname)}</b><p>${e(r.content)}</p><span class="small">${e(r.created_at)}</span></div>`).join('')||'<p class="small">暂时没有评价。</p>'}`);}
function messages(){
 const list=S.db.conversations.filter(c=>[c.initiator_id,c.peer_id].includes(S.currentUser));
 if(!list.some(c=>c.conversation_id===selectedChat))selectedChat=list[0]?.conversation_id;
 const current=list.find(c=>c.conversation_id===selectedChat),peer=current&&(current.initiator_id===S.currentUser?current.peer_id:current.initiator_id);
 app.innerHTML=title('消息','关于好物的故事，从一句“还在吗”开始。')+`<div class="panel chat"><aside class="chat-sidebar">${list.map(c=>{const id=c.initiator_id===S.currentUser?c.peer_id:c.initiator_id,last=S.db.messages.filter(m=>m.conversation_id===c.conversation_id).at(-1);return `<button class="chat-person ${selectedChat===c.conversation_id?'active':''}" data-action="chat" data-id="${c.conversation_id}"><b>${e(S.user(id)?.nickname)}</b><p>${e(last?.content||'开始聊聊吧')}</p></button>`;}).join('')||'<p class="small">还没有会话</p>'}</aside><section class="chat-main">${current?`<div class="chat-header">${e(S.user(peer)?.nickname)}<p class="small">接口聊天演示 · 切换身份可从另一方查看和回复</p></div><div class="chat-log">${S.db.messages.filter(m=>m.conversation_id===selectedChat).map(m=>`<div class="bubble ${m.sender_id===S.currentUser?'mine':''}"><time>${e(S.user(m.sender_id)?.nickname)} · ${e(m.sent_at.slice(5,16))}</time><p>${e(m.content)}</p></div>`).join('')}</div><form id="chat-form" class="chat-compose"><input name="content" aria-label="消息内容" placeholder="和对方聊聊这件好物…" required maxlength="500" autocomplete="off"><button class="primary">发送消息</button></form>`:empty('在商品详情中点击“联系卖家”，开始一段对话。')}</section></div>`;
 const log=document.querySelector('.chat-log');if(log)log.scrollTop=log.scrollHeight;
}
function profile(){
 const u=S.user();app.innerHTML=title('我的循物','整理生活，也给闲置一个新的开始。',`<button class="secondary" data-action="purchases">购买记录</button><button class="secondary" data-action="identity">账号与退出</button>`)+`<div class="two-column"><section class="panel"><div class="row"><span class="avatar" style="display:grid;place-items:center">${e(u.nickname[0])}</span><div><h3>${e(u.nickname)}</h3><span class="small">${e(u.phone)} · ${status(u.account_status)}</span></div></div><p class="hint">您已登录。个人地址、订单和消息仅对相关账号开放。</p><div class="row"><button class="secondary" data-action="address">＋ 添加地址</button></div></section><section class="panel"><h3>我的收货地址</h3>${S.db.addresses.filter(a=>a.user_id===S.currentUser).map(a=>`<div class="review"><b>${e(a.recipient_name)}</b> · ${e(a.recipient_phone)}<p>${e(a.detail_address)}</p><button class="inline-link" data-action="address" data-id="${a.address_id}">编辑地址</button></div>`).join('')||'<p class="small">还没有地址，购买快递商品前请先添加。</p>'}</section></div><div class="section-heading"><div><h2>我发布的闲置</h2><p>待审核、在售和交易中的物品都在这里。</p></div><div class="row"><button class="secondary" data-action="batch-delete">批量删除所选</button><button class="secondary" data-action="publish">＋ 发布</button></div></div><div class="product-grid">${S.db.products.filter(p=>p.seller_id===S.currentUser).map(p=>`<div><div class="row" style="margin-bottom:9px">${status(p.product_status)}${status(p.audit_status)}<label class="check"><input type="checkbox" name="selected-product" value="${p.product_id}" ${['锁定','已售'].includes(p.product_status)?'disabled':''}>选择</label></div>${productCard(p)}${p.audit_note?`<p class="small">审核意见：${e(p.audit_note)}</p>`:''}</div>`).join('')||empty('还没有发布闲置。')}</div>`;
}
function admin(){
 app.innerHTML=title('管理后台','商品审核、举报处理与退款记录。')+`<p class="hint">当前为管理员会话。审核通过的商品会进入市场，举报处理结果会保存到数据库。</p><div class="two-column"><section class="panel"><h3>待审核商品</h3>${S.db.products.filter(p=>p.audit_status==='待审核').map(p=>`<article class="admin-item"><div><span class="small">商品 #${p.product_id} · ${e(S.user(p.seller_id)?.nickname)}</span><h3>${e(p.title)}</h3><p class="small">${e(p.description)}</p><button class="inline-link" data-action="detail" data-id="${p.product_id}">查看详情与图片</button></div><div class="order-actions"><button class="secondary" data-action="audit-reject" data-id="${p.product_id}">驳回</button><button class="primary" data-action="audit-pass" data-id="${p.product_id}">通过</button></div></article>`).join('')||empty('待审核商品已全部处理。')}</section><section class="panel"><h3>商品举报</h3>${S.db.reports.map(r=>`<article class="admin-item"><div><span class="small">举报 #${r.report_id} · 商品 #${r.product_id}</span><h3>${e(S.product(r.product_id)?.title)}</h3><p class="small">${e(r.reason)}</p>${status(r.report_status)}<p class="small">${e(r.handle_note||'等待处理')}</p></div>${r.report_status==='待处理'?`<div class="order-actions"><button class="secondary" data-action="report-dismiss" data-id="${r.report_id}">不成立</button><button class="primary" data-action="report-uphold" data-id="${r.report_id}">成立并下架</button></div>`:''}</article>`).join('')||empty('暂时没有举报。')}</section></div><section class="panel" style="margin-top:22px"><h3>退款记录</h3>${S.db.refunds.map(r=>`<div class="review">订单 #${r.order_id} · ${e(r.reason)} ${status(r.refund_status)}<span class="small"> · 全额退款 ¥${money(findOrder(r.order_id).deal_price_cents+findOrder(r.order_id).shipping_fee_cents)}</span></div>`).join('')||'<p class="small">暂无退款申请</p>'}<p class="hint">本阶段由卖家在“我卖出的”中处理退款；管理员介入字段保留，暂不实现资金与物流流程。</p></section>`;
}
function identity(){if(!S.account?.role)return authPage();modal('当前账号',`<p>${e(S.account.nickname)} · ${S.account.role==='ADMIN'?'管理员':'普通用户'}</p><p class="small">退出后可登录其他账号。交易与聊天记录保存在数据库中。</p><button class="primary" data-action="logout">退出登录</button>`);}
function authPage(mode='login'){
 const register=mode==='register',adminLogin=mode==='admin';
 document.querySelector('#nav').hidden=true;document.querySelector('#publish-btn').hidden=true;
 document.querySelector('#identity-btn').textContent='登';
 app.innerHTML=`<section class="auth-layout"><div class="auth-intro"><div class="eyebrow">LESS WASTE, MORE STORIES</div><h1>好物再相遇，<br>从这里开始。</h1><p>发现闲置，分享生活。<br>让每件物品，都有下一段故事。</p><img src="/assets/camera.svg" alt="闲置相机插画"></div><section class="panel auth-card"><div class="tabs"><button class="tab ${mode==='login'?'active':''}" data-action="auth-mode" data-value="login">用户登录</button><button class="tab ${register?'active':''}" data-action="auth-mode" data-value="register">注册账号</button><button class="tab ${adminLogin?'active':''}" data-action="auth-mode" data-value="admin">管理员</button></div><h2>${register?'加入循物':adminLogin?'管理后台登录':'欢迎回来'}</h2><p class="small">${register?'一个账号，既可以买，也可以卖。':'使用您的账号继续。'}</p><form id="auth-form" data-mode="${mode}"><div class="form-grid">${register?field('nickname','昵称','','text','full'):''}<div class="field full"><label for="account">${adminLogin?'管理员账号':'手机号'}</label><input id="account" name="account" required maxlength="80" autocomplete="username" ${adminLogin?'':'inputmode="tel"'}></div><div class="field full"><label for="password">密码</label><input id="password" name="password" type="password" required minlength="8" maxlength="72" autocomplete="${register?'new-password':'current-password'}"></div>${register?'<div class="field full"><label for="password-confirm">再次输入密码</label><input id="password-confirm" name="password_confirm" type="password" required minlength="8" maxlength="72" autocomplete="new-password"></div>':''}</div><p class="error-box" id="auth-error" hidden role="alert"></p><button class="primary auth-submit">${register?'注册并登录':'登录'}</button></form><p class="small">课程实践项目 · 支付与物流为模拟流程</p></section></section>`;
}

function reasonForm(kind,id){const titles={report:'举报商品',refund:'申请全额退款',review:'评价这次交易','audit-reject':'填写驳回意见','report-uphold':'记录举报处理意见','report-dismiss':'记录举报处理意见'};modal(titles[kind],`<form id="reason-form" data-kind="${kind}" data-id="${id}"><div class="field"><label for="reason">${kind==='review'?'文字评价':'请说明原因或处理意见'}</label><textarea id="reason" name="reason" required maxlength="500"></textarea></div><div class="form-actions"><button class="secondary" type="button" data-action="close">取消</button><button class="primary">提交</button></div></form>`);}
function addressForm(id){const a=S.db.addresses.find(a=>a.address_id===Number(id)&&a.user_id===S.currentUser);modal(a?'编辑收货地址':'添加收货地址',`<form id="address-form" data-id="${a?.address_id||''}"><div class="form-grid">${field('recipient_name','收货人',a?.recipient_name||'')}${field('recipient_phone','收货电话',a?.recipient_phone||'','tel')}${field('detail_address','完整地址',a?.detail_address||'','text','full')}</div><p class="hint">更新个人地址不会修改已生成订单中的收货快照。</p><div class="form-actions"><button type="button" class="secondary" data-action="close">取消</button><button class="primary">保存地址</button></div></form>`);}
function confirmAction(heading,desc,action,id){modal(heading,`<p class="small">${desc}</p><div class="form-actions"><button class="secondary" data-action="close">返回</button><button class="primary" data-action="confirmed-${action}" data-id="${id||''}">确认</button></div>`);}
function navigate(route){if(location.hash==='#'+route)render();else location.hash=route;}
function render(){
 searchSequence++;
 if(!S.account?.role)return authPage(location.hash==='#admin'?'admin':'login');
 document.querySelector('#nav').hidden=S.account.role==='ADMIN';document.querySelector('#publish-btn').hidden=S.account.role==='ADMIN';
 const route=S.account.role==='ADMIN'?'admin':location.hash.slice(1).split('?')[0]||'discover';
 document.querySelectorAll('#nav a').forEach(a=>a.classList.toggle('active',a.hash==='#'+(route==='finds'?'discover':route)));
 document.querySelector('#identity-btn').textContent=S.account.nickname[0];document.querySelector('#identity-btn').title='当前账号：'+S.account.nickname;
 if(S.account.role==='ADMIN')admin();else if(route==='admin'){app.innerHTML=title('管理员入口','请退出普通用户账号，再使用管理员账号登录。','<button class="secondary" data-action="logout">退出并前往登录</button>');}else ({discover,finds:discover,favorites,orders,messages,profile}[route]||discover)();
}
document.addEventListener('click',async event=>{
 const btn=event.target.closest('[data-action]');if(!btn)return;
 const {action,id,value}=btn.dataset;
 if(btn.disabled)return;btn.disabled=true;
 try{
  if(action==='close')return close();
  if(action==='purchases'){
   const rows=await S.purchases();modal('我的购买记录',rows.map(o=>`<div class="review"><b>${e(o.snapshot_title)}</b><p>订单 #${o.order_id} · 卖家 ${e(o.seller_nickname)} · ¥${money(o.deal_price_cents+o.shipping_fee_cents)}</p>${status(o.order_status)}</div>`).join('')||empty('还没有购买记录。'));return;
  }
  if(action==='batch-delete'){
   pendingDelete=Array.from(document.querySelectorAll('input[name="selected-product"]:checked'),x=>Number(x.value));
   if(!pendingDelete.length)return toast('请先选择商品');
   return confirmAction('批量删除商品',`确认删除所选 ${pendingDelete.length} 件商品？任意一件有交易或举报历史时，整批不会删除。`,'batch-delete');
  }
  if(action==='category'){category=value;return discover();}
  if(action==='detail')return showDetail(id);
  if(action==='thumbnail'){document.querySelector('#detail-photo').src=S.images(Number(id))[Number(btn.dataset.index)];return;}
  if(action==='favorite'||action==='detail-favorite'){await S.favorite(Number(id));render();if(action==='detail-favorite')showDetail(id);return;}
  if(action==='buy')return buy(id);
  if(action==='publish'||action==='edit-product')return publish(id);
  if(action==='contact'){selectedChat=await S.conversation(Number(id));close();navigate('messages');return;}
  if(action==='role'){orderRole=orderRole==='buy'?'sell':'buy';orderTab='全部';return orders();}
  if(action==='order-tab'){orderTab=value;return orders();}
  if(action==='order-detail')return orderDetail(id);
  if(action==='chat'){selectedChat=Number(id);return messages();}
  if(action==='identity')return identity();
  if(action==='logout'){await S.logout();selectedChat=null;close();render();return;}
  if(action==='auth-mode')return authPage(value);
  if(action==='delete-product')return confirmAction('删除商品','删除后无法恢复；有订单或举报历史的商品不能删除。','delete-product',id);
  if(action==='address')return addressForm(id);
  if(['report','refund','review','audit-reject','report-uphold','report-dismiss'].includes(action))return reasonForm(action,id);
  if(['pay','cancel','receive','refund-agree','refund-reject'].includes(action))return confirmAction({pay:'模拟付款',cancel:'取消订单',receive:'确认收货','refund-agree':'同意全额退款','refund-reject':'拒绝退款'}[action],{pay:'仅更新演示订单状态，不会扣款。',cancel:'取消后商品将恢复为在售状态。',receive:'请确认本次演示订单已收货。','refund-agree':'订单将标记为已退款，商品下架。','refund-reject':'退款申请将标记为已拒绝，订单保持当前状态。'}[action],action,id);
  if(action.startsWith('confirmed-')){
   const op=action.replace('confirmed-','');
   if(op==='batch-delete'){await S.deleteProducts(pendingDelete);pendingDelete=[];}
   else if(op==='delete-product')await S.deleteProduct(Number(id));
   else if(op==='refund-agree'||op==='refund-reject')await S.decideRefund(Number(id),op==='refund-agree');
   else await S.transition(Number(id),op);
   close();render();toast('演示状态已更新');return;
  }
  if(action==='ship'){
   const o=findOrder(id);if(o.delivery_method==='自提')return confirmAction('确认自提交付','演示确认后订单完成，商品标记为已售。','ship',id);
   return modal('填写发货信息',`<form id="ship-form" data-id="${id}"><div class="form-grid">${field('carrier','快递公司')}${field('tracking_no','快递单号')}</div><div class="form-actions"><button class="primary">确认演示发货</button></div></form>`);
  }
  if(action==='audit-pass'){
   await S.audit(Number(id),true,'信息完整，审核通过');render();toast('审核通过，商品已上架');return;
  }
 }catch(err){if(document.querySelector('#modal').open)error(err.message);else toast(err.message);}finally{if(btn)btn.disabled=false;}
});
document.addEventListener('submit',async event=>{
 const form=event.target;if(!form.matches('form'))return;event.preventDefault();
 const btn=form.querySelector('button:not([type=button])');if(btn?.disabled)return;if(btn)btn.disabled=true;
 const f=new FormData(form),values=Object.fromEntries(f),id=Number(form.dataset.id);
 try{
  if(form.id==='auth-form'){
   const register=form.dataset.mode==='register';
   if(register&&values.password!==values.password_confirm)throw new Error('两次输入的密码不一致');
   await S.login(register?{nickname:values.nickname,phone:values.account,password:values.password}:{account:values.account,password:values.password,role:form.dataset.mode==='admin'?'ADMIN':'USER'},register);
   navigate(S.account.role==='ADMIN'?'admin':'discover');return;
  }
  if(form.id==='search-form'){if(values.minPrice!==''&&values.maxPrice!==''&&Number(values.minPrice)>Number(values.maxPrice))return toast('最低价格不能大于最高价格');query=(values.query||'').trim();minPrice=values.minPrice;maxPrice=values.maxPrice;return discover();}
  if(form.id==='buy-form'){await S.createOrder(id,values.delivery_method,Number(values.address_id));close();orderRole='buy';orderTab='全部';navigate('orders');return toast('演示订单已创建，等待模拟付款');}
  if(form.id==='publish-form'){
   if(!values.title.trim()||!values.description.trim())throw new Error('请填写标题和描述。');
   if(!f.has('support_pickup')&&!f.has('support_express'))throw new Error('请至少选择一种交付方式。');
   const price=Number(values.price),cents=Math.round(price*100);if(!Number.isFinite(price)||cents<1||cents>100000000)throw new Error('请输入有效价格。');
   await S.publish(id,{title:values.title.trim(),description:values.description.trim(),price_cents:cents,category:values.category,support_express:f.has('support_express'),support_pickup:f.has('support_pickup'),images:uploadImages});close();navigate('profile');return toast('提交成功，请等待管理员审核');
  }
  if(form.id==='chat-form'){const content=values.content.trim();if(!content)throw new Error('请输入消息内容。');const chat=selectedChat;await S.send(chat,content);if(location.hash==='#messages'&&selectedChat===chat){messages();document.querySelector('#chat-form input')?.focus();}return;}
  if(form.id==='address-form'){
   if(Object.values(values).some(v=>!v.trim()))throw new Error('请完整填写地址信息。');
   await S.address(id,values);close();render();return toast('地址已保存');
  }
  if(form.id==='ship-form'){await S.transition(id,'ship',values);close();render();return toast('演示订单已发货');}
  if(form.id==='reason-form'){
   const reason=values.reason.trim();if(!reason)throw new Error('请输入内容。');
   const kind=form.dataset.kind;
   if(kind==='refund')await S.requestRefund(id,reason);
   else if(kind==='review') await S.review(id,reason);
   else if(kind==='report') await S.report(id,reason);
   else if(kind==='audit-reject') await S.audit(id,false,reason);
   else await S.decideReport(id,kind==='report-uphold',reason);
   close();render();return toast('提交成功');
  }
 }catch(err){const box=document.querySelector('#auth-error');if(form.id==='auth-form'&&box){box.hidden=false;box.textContent=err.message;}else if(document.querySelector('#modal').open)error(err.message);else toast(err.message);}finally{if(btn)btn.disabled=false;}
});
document.addEventListener('change',async event=>{
 if(event.target.id==='sort-filter'){sort=event.target.value;discover();}
 if(event.target.id==='pickup-filter'){pickup=event.target.checked;discover();}
 if(event.target.id==='delivery')updateDelivery();
 if(event.target.id==='photo-files'){
  const button=document.querySelector('#publish-submit'),files=[...event.target.files];button.disabled=true;
  try{
   if(files.length>4)throw new Error('最多选择 4 张图片。');
   for(const f of files)if(!['image/png','image/jpeg','image/webp'].includes(f.type)||f.size>1024*1024)throw new Error('仅支持 1MB 以内的 PNG、JPEG、WebP 图片。');
   const result=await Promise.all(files.map(file=>new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(new Error('图片读取失败'));reader.readAsDataURL(file);})));if(result.length)uploadImages=result;
   document.querySelector('#upload-preview').innerHTML=uploadImages.map(src=>`<img src="${e(src)}" alt="商品预览">`).join('');
  }catch(err){error(err.message);event.target.value='';}finally{button.disabled=false;}
 }
});
document.querySelector('#publish-btn').addEventListener('click',()=>S.account?.role==='USER'?publish():authPage());
window.addEventListener('auth-expired',()=>{close();render();});
document.querySelector('#identity-btn').addEventListener('click',identity);
document.querySelector('#modal').addEventListener('click',event=>{if(event.target===event.currentTarget){const r=event.currentTarget.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)close();}});
window.addEventListener('hashchange',async()=>{try{await S.refresh();render();if(location.hash==='#finds')document.querySelector('#finds')?.scrollIntoView({behavior:'smooth'});else window.scrollTo(0,0);}catch(err){toast(err.message);}});
try{await S.initialize();render();}catch(err){app.innerHTML=`<div class="error-box">${e(err.message)}<p>请从 http://localhost:8080 访问，勿直接双击 HTML 文件。</p><button class="secondary" onclick="location.reload()">重新加载</button></div>`;}
