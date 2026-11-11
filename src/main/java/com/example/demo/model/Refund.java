package com.example.demo.model;

/** refunds：对应最终报告，数据访问只通过 Repository。 */
public class Refund {
  public Integer refund_id;
  public Integer order_id;
  public String reason;
  public String refund_status;
  public Integer admin_id;
  public String admin_opinion;
  public String created_at;
}
