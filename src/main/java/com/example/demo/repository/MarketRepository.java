package com.example.demo.repository;

import com.example.demo.mapper.*;
import com.example.demo.model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Typed Mapper dependencies and read-only page aggregation. No write-back snapshots. */
@Repository
public class MarketRepository {
  private final AdminMapper admin;
  private final UserMapper user;
  private final AddressMapper address;
  private final ProductMapper product;
  private final ProductImageMapper productImage;
  private final TradeOrderMapper tradeOrder;
  private final ConversationMapper conversation;
  private final MessageMapper message;
  private final RefundMapper refund;
  private final ReviewMapper review;
  private final ReportMapper report;
  private final FavoriteMapper favorite;

  public MarketRepository(
      AdminMapper admin,
      UserMapper user,
      AddressMapper address,
      ProductMapper product,
      ProductImageMapper productImage,
      TradeOrderMapper tradeOrder,
      ConversationMapper conversation,
      MessageMapper message,
      RefundMapper refund,
      ReviewMapper review,
      ReportMapper report,
      FavoriteMapper favorite) {
    this.admin = admin;
    this.user = user;
    this.address = address;
    this.product = product;
    this.productImage = productImage;
    this.tradeOrder = tradeOrder;
    this.conversation = conversation;
    this.message = message;
    this.refund = refund;
    this.review = review;
    this.report = report;
    this.favorite = favorite;
  }

  public AdminMapper admin() {
    return admin;
  }

  public UserMapper user() {
    return user;
  }

  public AddressMapper address() {
    return address;
  }

  public ProductMapper product() {
    return product;
  }

  public ProductImageMapper productImage() {
    return productImage;
  }

  public TradeOrderMapper tradeOrder() {
    return tradeOrder;
  }

  public ConversationMapper conversation() {
    return conversation;
  }

  public MessageMapper message() {
    return message;
  }

  public RefundMapper refund() {
    return refund;
  }

  public ReviewMapper review() {
    return review;
  }

  public ReportMapper report() {
    return report;
  }

  public FavoriteMapper favorite() {
    return favorite;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public MarketState snapshot() {
    MarketState s = new MarketState();
    s.admins = admin.findAll();
    s.users = user.findAll();
    s.addresses = address.findAll();
    s.products = product.findAll();
    s.product_images = productImage.findAll();
    s.orders = tradeOrder.findAll();
    s.conversations = conversation.findAll();
    s.messages = message.findAll();
    s.refunds = refund.findAll();
    s.reviews = review.findAll();
    s.reports = report.findAll();
    s.favorites = favorite.findAll();
    return s;
  }
}
