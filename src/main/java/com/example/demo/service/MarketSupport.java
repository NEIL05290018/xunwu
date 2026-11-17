package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 各业务服务共享的查找、输入检查和 DTO 转换。 */
public abstract class MarketSupport {
  protected final MarketRepository repository;
  protected final ObjectMapper mapper;

  protected MarketSupport(MarketRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  protected <T> T dto(Object value, Class<T> type) {
    return mapper.convertValue(value, type);
  }

  protected static void require(boolean ok, BusinessException.Kind kind, String message) {
    if (!ok) throw new BusinessException(kind, message);
  }

  protected static String text(String value, int max) {
    require(
        value != null && !value.isBlank() && value.trim().length() <= max,
        INVALID,
        "内容不能为空或超过长度限制");
    return value.trim();
  }

  protected static String now() {
    return LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  protected static User user(MarketState s, int id) {
    for (User x : s.users)
      if (x.user_id == id) {
        require("正常".equals(x.account_status), FORBIDDEN, "账号已停用");
        return x;
      }
    throw new BusinessException(NOT_FOUND, "用户不存在");
  }

  protected static Product product(MarketState s, int id) {
    for (Product x : s.products) if (x.product_id == id) return x;
    throw new BusinessException(NOT_FOUND, "商品不存在");
  }

  protected static TradeOrder order(MarketState s, int id) {
    for (TradeOrder x : s.orders) if (x.order_id == id) return x;
    throw new BusinessException(NOT_FOUND, "订单不存在");
  }

  protected static boolean participant(MarketState s, TradeOrder o, int uid) {
    return o.buyer_id == uid || product(s, o.product_id).seller_id == uid;
  }

  protected static void admin(MarketState s, int id) {
    for (Admin a : s.admins) if (a.admin_id == id) return;
    throw new BusinessException(FORBIDDEN, "管理员不存在");
  }

  protected static boolean pendingRefund(MarketState s, int id) {
    for (Refund r : s.refunds) if (r.order_id == id && "待处理".equals(r.refund_status)) return true;
    return false;
  }

  protected User user(int id) {
    User u = repository.user().findById(id);
    require(u != null, NOT_FOUND, "用户不存在");
    require("正常".equals(u.account_status), FORBIDDEN, "账号已停用");
    return u;
  }

  protected void admin(int id) {
    require(repository.admin().findById(id) != null, FORBIDDEN, "管理员不存在");
  }

  protected Product product(int id) {
    Product p = repository.product().findById(id);
    require(p != null, NOT_FOUND, "商品不存在");
    return p;
  }

  protected Product lockProduct(int id) {
    Product p = repository.product().lockById(id);
    require(p != null, NOT_FOUND, "商品不存在");
    return p;
  }

  protected TradeOrder lockOrder(int id) {
    TradeOrder o = repository.tradeOrder().lockById(id);
    require(o != null, NOT_FOUND, "订单不存在");
    return o;
  }

  protected boolean pendingRefund(int id) {
    Refund r = repository.refund().findByOrder(id);
    return r != null && "待处理".equals(r.refund_status);
  }
}
