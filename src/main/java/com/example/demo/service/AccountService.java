package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AccountService extends MarketSupport {
  public AccountService(MarketRepository repository, ObjectMapper mapper) {
    super(repository, mapper);
  }

  @org.springframework.transaction.annotation.Transactional(
      readOnly = true,
      isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
  public Bootstrap bootstrap(int uid, Integer aid) {
    MarketState s = repository.snapshot();
    if (aid != null) {
      admin(s, aid);
      return dto(s, Bootstrap.class);
    }
    user(s, uid);
    // 返回工作副本并过滤私有交易数据，不泄露其他用户地址或聊天。
    for (int index = s.addresses.size() - 1; index >= 0; index--) {
      Address a = s.addresses.get(index);
      if (a.user_id != uid) s.addresses.remove(index);
    }
    for (int index = s.orders.size() - 1; index >= 0; index--) {
      TradeOrder o = s.orders.get(index);
      if (!participant(s, o, uid)) s.orders.remove(index);
    }
    Set<Integer> orderIds = new HashSet<>();
    for (TradeOrder o : s.orders) orderIds.add(o.order_id);
    for (int index = s.refunds.size() - 1; index >= 0; index--) {
      Refund r = s.refunds.get(index);
      if (!orderIds.contains(r.order_id)) s.refunds.remove(index);
    }
    for (int index = s.reviews.size() - 1; index >= 0; index--) {
      Review r = s.reviews.get(index);
      if (!orderIds.contains(r.order_id)) s.reviews.remove(index);
    }
    for (int index = s.conversations.size() - 1; index >= 0; index--) {
      Conversation c = s.conversations.get(index);
      if (c.initiator_id != uid && c.peer_id != uid) s.conversations.remove(index);
    }
    Set<Integer> chatIds = new HashSet<>();
    for (Conversation c : s.conversations) chatIds.add(c.conversation_id);
    for (int index = s.messages.size() - 1; index >= 0; index--) {
      Message m = s.messages.get(index);
      if (!chatIds.contains(m.conversation_id)) s.messages.remove(index);
    }
    for (int index = s.reports.size() - 1; index >= 0; index--) {
      Report r = s.reports.get(index);
      if (r.reporter_id != uid) s.reports.remove(index);
    }
    Set<Integer> referenced = new HashSet<>();
    for (TradeOrder o : s.orders) referenced.add(o.product_id);
    for (int index = s.products.size() - 1; index >= 0; index--) {
      Product p = s.products.get(index);
      if (p.seller_id != uid && !"通过".equals(p.audit_status) && !referenced.contains(p.product_id))
        s.products.remove(index);
    }
    Set<Integer> productIds = new HashSet<>();
    for (Product p : s.products) productIds.add(p.product_id);
    for (int index = s.product_images.size() - 1; index >= 0; index--) {
      ProductImage i = s.product_images.get(index);
      if (!productIds.contains(i.product_id)) s.product_images.remove(index);
    }
    for (int index = s.favorites.size() - 1; index >= 0; index--) {
      Favorite f = s.favorites.get(index);
      if (!productIds.contains(f.product_id) || f.user_id != uid) s.favorites.remove(index);
    }
    s.admins.clear();
    for (User u : s.users) if (u.user_id != uid) u.phone = null;
    return dto(s, Bootstrap.class);
  }

  @org.springframework.transaction.annotation.Transactional
  public AddressResponse address(int uid, Integer id, AddressInput input) {
    user(uid);
    Address a;
    if (id == null) {
      a = new Address();
      a.user_id = uid;
    } else {
      a = repository.address().findById(id);
      require(a != null, NOT_FOUND, "地址不存在");
      require(a.user_id == uid, FORBIDDEN, "只能修改自己的地址");
    }
    a.recipient_name = text(input.recipient_name(), 80);
    a.recipient_phone = text(input.recipient_phone(), 80);
    a.detail_address = text(input.detail_address(), 200);
    if (id == null) repository.address().insert(a);
    else repository.address().update(a);
    return dto(a, AddressResponse.class);
  }
}
