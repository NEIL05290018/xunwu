package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class CommunityService extends MarketSupport {
  public CommunityService(MarketRepository repository, ObjectMapper mapper) {
    super(repository, mapper);
  }

  public ConversationResponse conversation(int uid, ConversationInput input) {
    require(input.peer_id() != null, INVALID, "请选择对方用户");
    int peer = input.peer_id();
    require(peer != uid, INVALID, "不能与自己建立会话");
    // 按编号顺序锁两个用户，避免 A→B 与 B→A 并发创建重复会话。
    repository.user().lockById(Math.min(uid, peer));
    repository.user().lockById(Math.max(uid, peer));
    user(uid);
    user(peer);
    Conversation c = repository.conversation().findPair(uid, peer);
    if (c == null) {
      c = new Conversation();
      c.initiator_id = uid;
      c.peer_id = peer;
      c.created_at = now();
      repository.conversation().insert(c);
    }
    return dto(c, ConversationResponse.class);
  }

  public MessageResponse send(int uid, int id, TextInput input) {
    user(uid);
    Conversation c = repository.conversation().findById(id);
    require(c != null, NOT_FOUND, "会话不存在");
    require(c.initiator_id == uid || c.peer_id == uid, FORBIDDEN, "当前用户不属于此会话");
    Message m = new Message();
    m.conversation_id = id;
    m.sender_id = uid;
    m.content = text(input.content(), 500);
    m.sent_at = now();
    repository.message().insert(m);
    return dto(m, MessageResponse.class);
  }

  public ReportResponse report(int uid, int id, TextInput input) {
    user(uid);
    Product p = lockProduct(id);
    require(p.seller_id != uid, INVALID, "不能举报自己的商品");
    require("通过".equals(p.audit_status), FORBIDDEN, "商品尚未公开");
    Report r = new Report();
    r.reporter_id = uid;
    r.product_id = id;
    r.reason = text(input.content(), 500);
    r.report_status = "待处理";
    r.created_at = now();
    repository.report().insert(r);
    return dto(r, ReportResponse.class);
  }

  public ReportResponse decideReport(int aid, int id, DecisionInput input) {
    admin(aid);
    Report initial = repository.report().findById(id);
    require(initial != null, NOT_FOUND, "举报不存在");
    Product p = lockProduct(initial.product_id);
    Report r = repository.report().lockById(id);
    require("待处理".equals(r.report_status), CONFLICT, "举报已处理");
    require(input.agree() != null, INVALID, "请选择处理结果");
    r.report_status = input.agree() ? "成立" : "不成立";
    r.admin_id = aid;
    r.handle_note = text(input.note(), 500);
    repository.report().update(r);
    if (input.agree()) {
      p.product_status = "下架";
      repository.product().update(p);
    }
    return dto(r, ReportResponse.class);
  }
}
