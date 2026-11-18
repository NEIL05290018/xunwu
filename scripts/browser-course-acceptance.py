"""v4.1 浏览器验收；运行在隔离验收数据库，会创建并删除测试草稿。"""
from playwright.sync_api import sync_playwright, expect
from pathlib import Path
import os, json, time
BASE=os.environ.get('ACCEPTANCE_URL','http://localhost:8080')
OUT=Path(__file__).resolve().parents[1]/'build/browser-course'
OUT.mkdir(parents=True,exist_ok=True)
errors=[]; passed=[]
with sync_playwright() as pw:
 browser=pw.chromium.launch(headless=True)
 ctx=browser.new_context(viewport={'width':1440,'height':1000})
 page=ctx.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
 page.goto(BASE)
 page.get_by_label('手机号',exact=True).fill('19900000001')
 page.get_by_label('密码',exact=True).fill(os.environ['ACCEPTANCE_USER_PASSWORD'])
 page.get_by_role('button',name='登录',exact=True).click()
 page.get_by_role('heading',name='发现你的下一件好物').wait_for()
 page.get_by_label('搜索闲置商品').fill('机械')
 page.get_by_label('最低价格（元）').fill('180')
 page.get_by_label('最高价格（元）').fill('180')
 with page.expect_response(lambda r:'/api/products?' in r.url and 'minPrice=18000' in r.url):
  page.get_by_role('button',name='搜索',exact=True).click()
 expect(page.locator('.product-card')).to_have_count(1)
 expect(page.locator('.product-card')).to_contain_text('机械键盘')
 page.locator('[data-action="detail"][data-id="101"]').first.click()
 expect(page.locator('#modal')).to_contain_text('机械键盘')
 page.locator('#modal [data-action="close"]').first.click()
 page.screenshot(path=str(OUT/'price-search.png'),full_page=True)
 passed.append('页面价格闭区间查询与JOIN商品详情')
 # 收藏页请求真实JOIN接口。
 page.evaluate("location.hash='favorites'")
 with page.expect_response(lambda r:'/api/queries/favorites' in r.url):page.reload()
 page.get_by_role('heading',name='我的收藏').wait_for()
 passed.append('收藏JOIN页面')
 page.evaluate("location.hash='profile'")
 page.get_by_role('button',name='购买记录',exact=True).click()
 expect(page.locator('#modal')).to_contain_text('我的购买记录')
 page.locator('#modal [data-action="close"]').first.click()
 passed.append('购买记录JOIN页面')
 names=['批量验收A-'+str(int(time.time())),'批量验收B-'+str(int(time.time()))]
 for name in names:
  page.locator('#publish-btn').click()
  page.get_by_label('商品标题').fill(name);page.get_by_label('商品描述').fill('批量删除临时草稿')
  page.get_by_label('价格（元）',exact=True).fill('12.34')
  page.get_by_role('button',name='提交审核',exact=True).click()
  page.get_by_role('button',name=name,exact=True).wait_for()
  wrapper=page.locator('.product-grid > div').filter(has=page.get_by_role('button',name=name,exact=True))
  wrapper.locator('input[name="selected-product"]').check()
 # 发布导致页面刷新，重新选择两个草稿。
 for name in names:
  page.locator('.product-grid > div').filter(has=page.get_by_role('button',name=name,exact=True)).locator('input[name="selected-product"]').check()
 page.get_by_role('button',name='批量删除所选',exact=True).click()
 expect(page.locator('#modal')).to_contain_text('2 件商品')
 page.get_by_role('button',name='确认',exact=True).click()
 for name in names:expect(page.get_by_role('button',name=name,exact=True)).to_have_count(0)
 passed.append('页面多选确认与批量删除')
 page.set_viewport_size({'width':390,'height':844})
 page.evaluate("location.hash='discover'")
 page.get_by_role('heading',name='发现你的下一件好物').wait_for()
 assert not page.evaluate('document.documentElement.scrollWidth>innerWidth')
 expect(page.get_by_label('最低价格（元）')).to_be_visible()
 page.screenshot(path=str(OUT/'mobile-price.png'),full_page=True)
 passed.append('手机价格输入可见且无横向溢出')
 assert not errors,errors
 browser.close()
result={'passed':passed,'javascript_errors':errors}
(OUT/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
print(json.dumps(result,ensure_ascii=False))
