# 课堂练习对应：v4.1

本次把 student 的课堂知识映射到商品与交易业务，不额外添加学生选课表。若老师要求提交指定 student/sc/course 数据库作业，需另交该练习。

## 1. CRUD 与测试

`ProductMapper.java` 和 `ProductMapper.xml` 提供 insert、findById、findAll、findByCategory、update、deleteById。XML namespace 与接口全限定名对应，id 与方法名对应，resultMap 对应实体字段；insert 使用 useGeneratedKeys 回填主键。

`CourseMyBatisTests.mapperCrudAndGeneratedKeys` 直接执行 Mapper，依次验证新增、读取、列表、分类、修改、删除及主键回填。这些连接实际 MySQL 的测试属于数据库集成测试，区别于只 Mock 方法的单元测试。

## 2. 动态条件查询

`GET /api/products?scope=market&q=机械&category=数码配件&minPrice=10000&maxPrice=20000`

接口金额单位为分，页面输入为元，由前端转为分。两端都不能把价格当浮点金额直接存库。上下限可省略，包含边界；不允许负数、上下限倒置、超过100000000分。未指定可选条件仍保留公开/归属权限条件。

`CatalogController.find → CatalogService.find → ProductMapper.searchRange → ProductMapper.xml`。

XML 中 `<if>` 控制关键词、分类、最低价、最高价是否加入 SQL；`<where>` 处理 WHERE 和前导 AND；`<choose>` 控制 market/mine/all 权限范围。关键词使用绑定参数的 LOCATE，按字面包含匹配，百分号不会成为通配符。

首页搜索已经请求此接口。自提、排序及“更多分类”分组在返回结果上处理；其余组合条件在 MySQL 执行。原四参数 search 方法保留兼容已有练习与测试。

## 3. foreach 批量删除

`POST /api/products/batch-delete`，JSON 为 `{"ids":[118,119]}`，成功返回 `{"deleted":2}`。沿用登录会话和 X-CSRF-Token。

“我的”页勾选商品 → 批量删除所选 → 确认。最多100个输入编号，去重并升序锁定商品。Service 先检查所有编号存在、都属于当前用户、没有交易/举报历史且不处于锁定/已售状态，然后删除图片/收藏，最后执行 `<foreach>` 生成的 `DELETE ... IN (...)`。任一步失败事务回滚；SQL 空列表走 `WHERE 1=0`，Service 则直接拒绝空列表。

测试验证去重、关联清理、无权限、缺失编号、历史记录、空值、超长列表、CSRF，以及最后一条 SQL 异常后图片/收藏也被恢复。

## 4. JOIN 多表查询

| 接口 | 关联表 | 页面 |
|---|---|---|
| GET /api/queries/products/{id} | products + users | 商品详情 |
| GET /api/queries/favorites | favorites + products + users | 我的收藏，显示收藏时间 |
| GET /api/queries/purchases | orders + products + users | 我的 → 购买记录 |

对应 `MarketQueryController → MarketQueryService → MarketQueryMapper.java/XML`。返回 `JoinedProduct` / `PurchaseSummary`，只投影公开昵称和列表需要的字段，不返回密码或卖家手机号。所有用户编号来自会话，不接受请求指定其他用户；收藏和商品详情保留可见性条件；购买记录只查询当前买家。

订单名称使用成交快照 snapshot_title，不能用商品当前名称替代历史成交信息。原“我的订单”操作页面保留，避免改变既有买卖双方的交易按钮和退款流程。

## 5. 验证与学习建议

运行完整测试：`gradlew.bat test bootJar`（先配置独立 `_test` 结尾的 MySQL 库和 TEST_DB_PASSWORD）。新增类 CourseMyBatisTests 包含7项测试。

对照 XML 的条件，分别尝试不传条件、只传最低价、只传最高价、完整组合。随后从 Controller 追踪到 SQL，最后阅读批量删除事务测试，观察为何不能先删一件再单独提交下一件。
