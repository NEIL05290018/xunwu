package com.example.demo.model;

/** reviews：对应最终报告，数据访问只通过 Repository。 */
public class Review {
  public Integer review_id;
  public Integer order_id;
  public Integer reviewer_id;
  public String content;
  public String created_at;
}
