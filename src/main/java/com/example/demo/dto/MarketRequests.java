package com.example.demo.dto;

import java.util.List;

/** 请求 DTO 只接收用户可编辑字段；卖家、买家、状态、编号均由服务器确定。 */
public final class MarketRequests {
  public record ProductInput(
      String title,
      String description,
      Integer price_cents,
      String category,
      Boolean support_express,
      Boolean support_pickup,
      List<String> images) {}

  public record AddressInput(
      String recipient_name, String recipient_phone, String detail_address) {}

  public record OrderInput(Integer product_id, String delivery_method, Integer address_id) {}

  public record ShippingInput(String carrier, String tracking_no) {}

  public record TextInput(String content) {}

  public record ConversationInput(Integer peer_id) {}

  public record DecisionInput(Boolean agree, String note) {}
}
