# REST API

JSON 编码 UTF-8，金额单位分。先 GET `/api/auth/session`，浏览器获得 Cookie 和 csrf；所有 POST/PUT/DELETE 带 `X-CSRF-Token`。登录后会轮换会话和 token。客户端不提供 userId/adminId；旧演示头无效。

| 方法 | 路径 | 身份/作用 |
|---|---|---|
| GET | /api/auth/session | 获取会话信息与 csrf；未登录 role 为 null |
| POST | /api/auth/register | nickname、phone、password；注册后自动登录 |
| POST | /api/auth/login | account、password、role（USER/ADMIN） |
| POST | /api/auth/logout | 退出，204 |
| GET | /api/market | 普通用户过滤隐私；管理员获得管理视图 |
| GET | /api/products | 普通用户；category、q、scope=market/all/mine |
| GET | /api/products/{id} | 商品详情，未公开草稿仅本人可见 |
| POST / PUT | /api/products、/api/products/{id} | 发布/编辑后待审核 |
| DELETE | /api/products/{id} | 删除自己的无历史商品 |
| PUT / DELETE | /api/products/{id}/favorite | 收藏/取消收藏 |
| POST / PUT | /api/addresses、/api/addresses/{id} | 本人地址维护 |
| GET / POST | /api/orders | 相关订单列表/下单 |
| POST | /api/orders/{id}/actions/{action} | pay/cancel/ship/receive，按买卖双方校验 |
| POST | /api/orders/{id}/refunds | 买家申请整单退款 |
| PUT | /api/refunds/{id} | 卖家同意/拒绝退款 |
| POST | /api/orders/{id}/reviews | 完成后双方各一次文字评价 |
| POST | /api/conversations | peer_id，查找或建立会话 |
| POST | /api/conversations/{id}/messages | 会话成员发送 content |
| POST | /api/products/{id}/reports | 举报 content |
| PUT | /api/admin/products/{id}/audit | 管理员；agree、note |
| PUT | /api/admin/reports/{id} | 管理员；agree、note |

200 查询/更新；201 新建；204 成功无内容；400 参数错误；401 未登录/登录失败；403 身份或 CSRF 不符；404 不存在；409 状态或数据库约束冲突。

商品 GET/PUT/DELETE 不存在时显式返回空 404；其他业务错误通常为 `{ "code": "...", "message": "..." }`。不要假设所有响应都能 `response.json()`。

请求字段完整定义：`dto/MarketRequests.java` 及 `AuthController` 中的 record。响应定义：`dto/MarketResponses.java`。可直接执行的例子见 `requests/`。

## v4.1 新增

- `GET /api/products` 增加可选整数 `minPrice` / `maxPrice`（分，闭区间，0—100000000）。保留 q/category/scope。
- `POST /api/products/batch-delete`：`{"ids":[118,119]}`，最多100项，返回 `{"deleted":2}`；去重，整批事务。空/非法输入400，无权限403，不存在404，有交易/举报历史409。
- `GET /api/queries/products/{id}`：商品与公开卖家昵称，未公开且非本人返回404。
- `GET /api/queries/favorites`：当前用户收藏及商品、卖家昵称、收藏时间。
- `GET /api/queries/purchases`：当前用户购买记录及卖家昵称，名称使用订单快照。

新接口均需普通用户登录，写入需 X-CSRF-Token。身份以服务器会话为准，不接受 uid 参数切换用户。详见 `课堂练习-MyBatis进阶.md`。
