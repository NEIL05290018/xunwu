from playwright.sync_api import sync_playwright,expect
import json,time,os
from pathlib import Path
OUTPUT=Path(__file__).resolve().parents[1]/"build/browser-acceptance"
OUTPUT.mkdir(parents=True,exist_ok=True)
BASE=os.environ.get("ACCEPTANCE_URL","http://localhost:8080")
USER_PASSWORD=os.environ["ACCEPTANCE_USER_PASSWORD"]
ADMIN_PASSWORD=os.environ["ACCEPTANCE_ADMIN_PASSWORD"]
results=[];errors=[]
def login(browser,account,password,admin=False):
 ctx=browser.new_context(viewport={'width':1440,'height':1000});page=ctx.new_page();page.on('pageerror',lambda e:errors.append(str(e)));page.goto(BASE)
 if admin:page.get_by_role('button',name='管理员',exact=True).click()
 page.get_by_label('管理员账号' if admin else '手机号').fill(account);page.get_by_label('密码',exact=True).fill(password);page.get_by_role('button',name='登录',exact=True).click()
 page.get_by_role('heading',name='管理后台' if admin else '发现你的下一件好物').wait_for();return ctx,page
def nav(page,hash):page.goto(BASE+'/#'+hash);page.wait_for_timeout(400)
def action(page,name):page.get_by_role('button',name=name,exact=True).click()
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,args=['--no-sandbox'])
 sellerctx,seller=login(browser,'19900000002',USER_PASSWORD);buyerctx,buyer=login(browser,'19900000001',USER_PASSWORD);adminctx,admin=login(browser,'admin01',ADMIN_PASSWORD,True)
 title='浏览器验收台灯-'+str(int(time.time()))
 seller.locator('#publish-btn').click();seller.get_by_label('商品标题').fill(title);seller.get_by_label('商品描述').fill('灯光正常，底座有轻微划痕。验收测试商品。');seller.get_by_label('价格（元）').fill('45');seller.get_by_label('商品分类').select_option('生活用品');action(seller,'提交审核');expect(seller.get_by_role('heading',name='我的循物')).to_be_visible();expect(seller.get_by_role('button',name=title,exact=True)).to_be_visible();results.append('浏览器发布商品')
 admin.reload();item=admin.locator('.admin-item').filter(has=admin.get_by_role('heading',name=title,exact=True));item.get_by_role('button',name='通过',exact=True).click();expect(item).to_have_count(0);results.append('管理员审核')
 buyer.reload();buyer.get_by_role('button',name=title,exact=True).click();action(buyer,'收藏好物');action(buyer,'联系卖家');buyer.get_by_label('消息内容').fill('你好，台灯还在吗？') if buyer.get_by_label('消息内容').count() else buyer.locator('#chat-form input').fill('你好，台灯还在吗？');action(buyer,'发送消息');expect(buyer.locator('.chat-log')).to_contain_text('你好，台灯还在吗？');results.append('收藏和聊天')
 nav(buyer,'discover');buyer.get_by_role('button',name=title,exact=True).click();action(buyer,'我想要');buyer.get_by_label('交付方式').select_option('快递');buyer.get_by_label('快递收货地址').select_option('1');action(buyer,'提交演示订单');row=buyer.locator('.order-row').filter(has=buyer.get_by_role('heading',name=title,exact=True));row.get_by_role('button',name='模拟付款',exact=True).click();action(buyer,'确认');expect(row).to_contain_text('待交付');results.append('下单与模拟付款')
 nav(seller,'orders');action(seller,'切换到我卖出的 ⇄');row=seller.locator('.order-row').filter(has=seller.get_by_role('heading',name=title,exact=True));row.get_by_role('button',name='填写发货信息').click();seller.get_by_label('快递公司').fill('课程快递');seller.get_by_label('快递单号').fill('DEMO123456');action(seller,'确认演示发货');expect(row).to_contain_text('待收货');results.append('卖家发货')
 buyer.reload();row=buyer.locator('.order-row').filter(has=buyer.get_by_role('heading',name=title,exact=True));row.get_by_role('button',name='确认收货').click();action(buyer,'确认');expect(row).to_contain_text('已完成');row.get_by_role('button',name='写评价').click();buyer.get_by_label('文字评价').fill('描述准确，沟通顺利。');action(buyer,'提交');row.get_by_role('button',name='订单详情').click();expect(buyer.locator('#modal')).to_contain_text('描述准确，沟通顺利。');buyer.screenshot(path=str(OUTPUT/'trade.png'),full_page=True);results.append('确认收货和文字评价')
 # Register through UI, then publish and delete a disposable draft.
 ctx=browser.new_context(viewport={'width':390,'height':844});page=ctx.new_page();page.on('pageerror',lambda e:errors.append(str(e)));page.goto(BASE);action(page,'注册账号');page.get_by_label('昵称').fill('验收同学');phone='188'+str(int(time.time()))[-8:];page.get_by_label('手机号').fill(phone);page.get_by_label('密码',exact=True).fill('BrowserUser_2026');page.get_by_label('再次输入密码').fill('BrowserUser_2026');action(page,'注册并登录');page.get_by_role('heading',name='发现你的下一件好物').wait_for();results.append('手机注册')
 page.locator('#publish-btn').click();page.get_by_label('商品标题').fill('待删除验收商品');page.get_by_label('商品描述').fill('这是用于删除流程的测试商品');page.get_by_label('价格（元）').fill('9.9');action(page,'提交审核');page.get_by_role('button',name='待删除验收商品',exact=True).click();action(page,'删除商品');action(page,'确认');expect(page.get_by_role('button',name='待删除验收商品',exact=True)).to_have_count(0);results.append('手机发布、删除')
 page.locator('#identity-btn').click();action(page,'退出登录');expect(page.get_by_role('heading',name='欢迎回来')).to_be_visible();assert not page.evaluate('document.documentElement.scrollWidth>innerWidth');page.screenshot(path=str(OUTPUT/'mobile-login.png'),full_page=True);results.append('手机退出、无横向溢出')
 assert not errors,errors
 print(json.dumps({'passed':results,'javascript_errors':errors,'persistent_product':title},ensure_ascii=False,indent=2));open(str(OUTPUT/'browser-result.json'),'w').write(json.dumps({'passed':results,'javascript_errors':errors,'persistent_product':title},ensure_ascii=False,indent=2));browser.close()
