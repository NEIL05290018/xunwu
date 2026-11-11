package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class CommunityService extends MarketSupport {
  public CommunityService(MarketRepository repository, ObjectMapper mapper) {
    super(repository, mapper);
  }

  public ConversationResponse conversation(int uid, ConversationInput input) {
    return repository.transaction(
        new MarketRepository.Operation<ConversationResponse>() {
          @Override
          public ConversationResponse apply(MarketState s) {
            user(s, uid);
            require(input.peer_id() != null, INVALID, "请选择对方用户");
            int peer = input.peer_id();
            user(s, peer);
            require(peer != uid, INVALID, "不能与自己建立会话");
            for (Conversation c : s.conversations)
              if ((c.initiator_id == uid && c.peer_id == peer)
                  || (c.initiator_id == peer && c.peer_id == uid))
                return dto(c, ConversationResponse.class);
            Conversation c = new Conversation();
            c.conversation_id = nextConversationId(s.conversations);
            c.initiator_id = Math.min(uid, peer);
            c.peer_id = Math.max(uid, peer);
            c.created_at = now();
            s.conversations.add(c);
            return dto(c, ConversationResponse.class);
          }
        });
  }

  public MessageResponse send(int uid, int id, TextInput input) {
    return repository.transaction(
        new MarketRepository.Operation<MessageResponse>() {
          @Override
          public MessageResponse apply(MarketState s) {
            user(s, uid);
            Conversation c = null;
            for (Conversation x : s.conversations) if (x.conversation_id == id) c = x;
            require(c != null, NOT_FOUND, "会话不存在");
            require(c.initiator_id == uid || c.peer_id == uid, FORBIDDEN, "当前用户不属于此会话");
            Message m = new Message();
            m.message_id = nextMessageId(s.messages);
            m.conversation_id = id;
            m.sender_id = uid;
            m.content = text(input.content(), 500);
            m.sent_at = now();
            s.messages.add(m);
            return dto(m, MessageResponse.class);
          }
        });
  }

  public ReportResponse report(int uid, int id, TextInput input) {
    return repository.transaction(
        new MarketRepository.Operation<ReportResponse>() {
          @Override
          public ReportResponse apply(MarketState s) {
            user(s, uid);
            Product p = product(s, id);
            require(p.seller_id != uid, INVALID, "不能举报自己的商品");
            require("通过".equals(p.audit_status), FORBIDDEN, "商品尚未公开");
            Report r = new Report();
            r.report_id = nextReportId(s.reports);
            r.reporter_id = uid;
            r.product_id = id;
            r.reason = text(input.content(), 500);
            r.report_status = "待处理";
            r.created_at = now();
            s.reports.add(r);
            return dto(r, ReportResponse.class);
          }
        });
  }

  public ReportResponse decideReport(int aid, int id, DecisionInput input) {
    return repository.transaction(
        new MarketRepository.Operation<ReportResponse>() {
          @Override
          public ReportResponse apply(MarketState s) {
            admin(s, aid);
            Report r = null;
            for (Report x : s.reports) if (x.report_id == id) r = x;
            require(r != null, NOT_FOUND, "举报不存在");
            require("待处理".equals(r.report_status), CONFLICT, "举报已处理");
            require(input.agree() != null, INVALID, "请选择处理结果");
            r.report_status = input.agree() ? "成立" : "不成立";
            r.admin_id = aid;
            r.handle_note = text(input.note(), 500);
            if (input.agree()) product(s, r.product_id).product_status = "下架";
            return dto(r, ReportResponse.class);
          }
        });
  }
}
