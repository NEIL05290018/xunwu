package com.example.demo.model;

/** users：对应最终报告，数据访问只通过 Repository。 */
public class User {
  public Integer user_id;
  public String nickname;
  public String phone;
  public String account_status;
  @com.fasterxml.jackson.annotation.JsonIgnore public String password_hash;
}
