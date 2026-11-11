package com.example.demo.dto;

import java.util.List;

/** 对外响应 DTO，不直接返回 Repository 的可变模型。 */
public final class MarketResponses {
  public record AdminResponse(Integer admin_id, String login_name) {}

  public record UserResponse(
      Integer user_id, String nickname, String phone, String account_status) {}

  public record AddressResponse(
      Integer address_id,
      Integer user_id,
      String recipient_name,
      String recipient_phone,
      String detail_address) {}

  public record ProductResponse(
      Integer product_id,
      Integer seller_id,
      String title,
      String description,
      Integer price_cents,
      String category,
      Integer support_express,
      Integer support_pickup,
      String product_status,
      String audit_status,
      Integer audit_admin_id,
      String audit_note) {}

  public record ProductImageResponse(Integer image_id, Integer product_id, String image_url) {}

  public record TradeOrderResponse(
      Integer order_id,
      Integer product_id,
      Integer buyer_id,
      String snapshot_title,
      Integer deal_price_cents,
      Integer shipping_fee_cents,
      String delivery_method,
      String recipient_name,
      String recipient_phone,
      String recipient_address,
      String order_status,
      String carrier,
      String tracking_no,
      String created_at) {}

  public record ConversationResponse(
      Integer conversation_id, Integer initiator_id, Integer peer_id, String created_at) {}

  public record MessageResponse(
      Integer message_id,
      Integer conversation_id,
      Integer sender_id,
      String content,
      String sent_at) {}

  public record RefundResponse(
      Integer refund_id,
      Integer order_id,
      String reason,
      String refund_status,
      Integer admin_id,
      String admin_opinion,
      String created_at) {}

  public record ReviewResponse(
      Integer review_id,
      Integer order_id,
      Integer reviewer_id,
      String content,
      String created_at) {}

  public record ReportResponse(
      Integer report_id,
      Integer reporter_id,
      Integer product_id,
      String reason,
      String report_status,
      Integer admin_id,
      String handle_note,
      String created_at) {}

  public record FavoriteResponse(Integer user_id, Integer product_id, String favorited_at) {}

  public record Bootstrap(
      List<AdminResponse> admins,
      List<UserResponse> users,
      List<AddressResponse> addresses,
      List<ProductResponse> products,
      List<ProductImageResponse> product_images,
      List<TradeOrderResponse> orders,
      List<ConversationResponse> conversations,
      List<MessageResponse> messages,
      List<RefundResponse> refunds,
      List<ReviewResponse> reviews,
      List<ReportResponse> reports,
      List<FavoriteResponse> favorites) {}

  public record ErrorResponse(String code, String message) {}
}
