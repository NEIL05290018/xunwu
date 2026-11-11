package com.example.demo.model;

/** orders：对应最终报告，数据访问只通过 Repository。 */
public class TradeOrder {
  public Integer order_id;
  public Integer product_id;
  public Integer buyer_id;
  public String snapshot_title;
  public Integer deal_price_cents;
  public Integer shipping_fee_cents;
  public String delivery_method;
  public String recipient_name;
  public String recipient_phone;
  public String recipient_address;
  public String order_status;
  public String carrier;
  public String tracking_no;
  public String created_at;
}
