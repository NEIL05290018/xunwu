package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

  protected static int nextTradeOrderId(List<TradeOrder> values) {
    int max = 0;
    for (TradeOrder x : values) max = Math.max(max, x.order_id);
    return max + 1;
  }

  protected static int nextRefundId(List<Refund> values) {
    int max = 0;
    for (Refund x : values) max = Math.max(max, x.refund_id);
    return max + 1;
  }

  protected static int nextReviewId(List<Review> values) {
    int max = 0;
    for (Review x : values) max = Math.max(max, x.review_id);
    return max + 1;
  }

  protected static int nextProductImageId(List<ProductImage> values) {
    int max = 0;
    for (ProductImage x : values) max = Math.max(max, x.image_id);
    return max + 1;
  }

  protected static int nextAddressId(List<Address> values) {
    int max = 0;
    for (Address x : values) max = Math.max(max, x.address_id);
    return max + 1;
  }

  protected static int nextConversationId(List<Conversation> values) {
    int max = 0;
    for (Conversation x : values) max = Math.max(max, x.conversation_id);
    return max + 1;
  }

  protected static int nextMessageId(List<Message> values) {
    int max = 0;
    for (Message x : values) max = Math.max(max, x.message_id);
    return max + 1;
  }

  protected static int nextReportId(List<Report> values) {
    int max = 0;
    for (Report x : values) max = Math.max(max, x.report_id);
    return max + 1;
  }
}
