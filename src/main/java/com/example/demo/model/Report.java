package com.example.demo.model;

/** reports：对应最终报告，数据访问只通过 Repository。 */
public class Report {
  public Integer report_id;
  public Integer reporter_id;
  public Integer product_id;
  public String reason;
  public String report_status;
  public Integer admin_id;
  public String handle_note;
  public String created_at;
}
