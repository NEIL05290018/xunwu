package com.example.demo.dto;

/** 购买记录只投影列表所需字段，商品名称使用成交快照。 */
public class PurchaseSummary {
  public Integer order_id, product_id, buyer_id, seller_id, deal_price_cents, shipping_fee_cents;
  public String snapshot_title, seller_nickname, order_status, created_at;
}
