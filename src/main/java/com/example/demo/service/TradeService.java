package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TradeService extends MarketSupport {
  public TradeService(MarketRepository repository, ObjectMapper mapper) {
    super(repository, mapper);
  }

  public TradeOrderResponse create(int uid, OrderInput input) {
    return repository.transaction(
        new MarketRepository.Operation<TradeOrderResponse>() {
          @Override
          public TradeOrderResponse apply(MarketState s) {
            user(s, uid);
            require(input.product_id() != null, INVALID, "请提供商品编号");
            Product p = product(s, input.product_id());
            require(p.seller_id != uid, FORBIDDEN, "不能购买自己的商品");
            require(
                "通过".equals(p.audit_status) && "在售".equals(p.product_status), CONFLICT, "商品当前不可购买");
            for (TradeOrder old : s.orders)
              require(
                  old.product_id.intValue() != p.product_id
                      || List.of("已取消", "已退款").contains(old.order_status),
                  CONFLICT,
                  "该商品已有有效订单");
            boolean express = "快递".equals(input.delivery_method());
            require(express || "自提".equals(input.delivery_method()), INVALID, "交付方式无效");
            require(
                express ? p.support_express == 1 : p.support_pickup == 1, INVALID, "商品不支持此交付方式");
            Address address = null;
            if (express) {
              for (Address a : s.addresses)
                if (a.address_id.equals(input.address_id()) && a.user_id == uid) address = a;
              require(address != null, INVALID, "请选择自己的收货地址");
            }
            TradeOrder o = new TradeOrder();
            o.order_id = nextTradeOrderId(s.orders);
            o.product_id = p.product_id;
            o.buyer_id = uid;
            o.snapshot_title = p.title;
            o.deal_price_cents = p.price_cents;
            o.shipping_fee_cents = express ? 600 : 0;
            o.delivery_method = input.delivery_method();
            o.order_status = "待付款";
            o.created_at = now();
            if (express) {
              o.recipient_name = address.recipient_name;
              o.recipient_phone = address.recipient_phone;
              o.recipient_address = address.detail_address;
            }
            s.orders.add(o);
            p.product_status = "锁定";
            return dto(o, TradeOrderResponse.class);
          }
        });
  }

  public TradeOrderResponse transition(int uid, int id, String action, ShippingInput shipping) {
    return repository.transaction(
        new MarketRepository.Operation<TradeOrderResponse>() {
          @Override
          public TradeOrderResponse apply(MarketState s) {
            user(s, uid);
            TradeOrder o = order(s, id);
            Product p = product(s, o.product_id);
            boolean seller = p.seller_id == uid, buyer = o.buyer_id == uid;
            require(seller || buyer, FORBIDDEN, "不能操作他人的订单");
            switch (action) {
              case "pay" -> {
                require(buyer, FORBIDDEN, "仅买家可付款");
                require("待付款".equals(o.order_status), CONFLICT, "订单不在待付款状态");
                o.order_status = "待交付";
              }
              case "cancel" -> {
                require(buyer, FORBIDDEN, "仅买家可取消");
                require("待付款".equals(o.order_status), CONFLICT, "已付款订单请申请退款");
                o.order_status = "已取消";
                boolean reported = false;
                for (Report r : s.reports)
                  if (r.product_id.equals(p.product_id) && "成立".equals(r.report_status))
                    reported = true;
                p.product_status = "通过".equals(p.audit_status) && !reported ? "在售" : "下架";
              }
              case "ship" -> {
                require(seller, FORBIDDEN, "仅卖家可交付");
                require("待交付".equals(o.order_status) && !pendingRefund(s, id), CONFLICT, "当前不能交付");
                if ("自提".equals(o.delivery_method)) {
                  o.order_status = "已完成";
                  p.product_status = "已售";
                } else {
                  require(shipping != null, INVALID, "请填写物流信息");
                  o.carrier = text(shipping.carrier(), 80);
                  o.tracking_no = text(shipping.tracking_no(), 80);
                  o.order_status = "待收货";
                }
              }
              case "receive" -> {
                require(buyer, FORBIDDEN, "仅买家可确认收货");
                require(
                    "待收货".equals(o.order_status) && !pendingRefund(s, id), CONFLICT, "当前不能确认收货");
                o.order_status = "已完成";
                p.product_status = "已售";
              }
              default -> throw new BusinessException(INVALID, "不支持的订单操作");
            }
            return dto(o, TradeOrderResponse.class);
          }
        });
  }

  public RefundResponse refund(int uid, int id, TextInput input) {
    return repository.transaction(
        new MarketRepository.Operation<RefundResponse>() {
          @Override
          public RefundResponse apply(MarketState s) {
            user(s, uid);
            TradeOrder o = order(s, id);
            require(o.buyer_id == uid, FORBIDDEN, "仅买家可申请退款");
            require(
                List.of("待交付", "待收货", "已完成").contains(o.order_status), CONFLICT, "未付款或已结束的订单不能退款");
            for (Refund old : s.refunds) require(old.order_id != id, CONFLICT, "该订单已有退款记录");
            Refund r = new Refund();
            r.refund_id = nextRefundId(s.refunds);
            r.order_id = id;
            r.reason = text(input.content(), 500);
            r.refund_status = "待处理";
            r.created_at = now();
            s.refunds.add(r);
            return dto(r, RefundResponse.class);
          }
        });
  }

  public RefundResponse decideRefund(int uid, int id, DecisionInput input) {
    return repository.transaction(
        new MarketRepository.Operation<RefundResponse>() {
          @Override
          public RefundResponse apply(MarketState s) {
            user(s, uid);
            Refund r = null;
            for (Refund x : s.refunds) if (x.refund_id == id) r = x;
            require(r != null, NOT_FOUND, "退款记录不存在");
            TradeOrder o = order(s, r.order_id);
            Product p = product(s, o.product_id);
            require(p.seller_id == uid, FORBIDDEN, "仅订单卖家可处理退款");
            require("待处理".equals(r.refund_status), CONFLICT, "退款已处理");
            require(input.agree() != null, INVALID, "请选择处理结果");
            r.refund_status = input.agree() ? "退款成功" : "已拒绝";
            if (input.agree()) {
              o.order_status = "已退款";
              p.product_status = "下架";
            }
            return dto(r, RefundResponse.class);
          }
        });
  }

  public ReviewResponse review(int uid, int id, TextInput input) {
    return repository.transaction(
        new MarketRepository.Operation<ReviewResponse>() {
          @Override
          public ReviewResponse apply(MarketState s) {
            user(s, uid);
            TradeOrder o = order(s, id);
            require(participant(s, o, uid), FORBIDDEN, "只能评价自己的交易");
            require("已完成".equals(o.order_status), CONFLICT, "完成交易后才可评价");
            for (Review old : s.reviews)
              require(!(old.order_id == id && old.reviewer_id == uid), CONFLICT, "请勿重复评价");
            Review r = new Review();
            r.review_id = nextReviewId(s.reviews);
            r.order_id = id;
            r.reviewer_id = uid;
            r.content = text(input.content(), 500);
            r.created_at = now();
            s.reviews.add(r);
            return dto(r, ReviewResponse.class);
          }
        });
  }
}
