package com.example.demo.model;

/** admins：对应最终报告，数据访问只通过 Repository。 */
public class Admin {
  public Integer admin_id;
  public String login_name;
  @com.fasterxml.jackson.annotation.JsonIgnore public String password_hash;
}
