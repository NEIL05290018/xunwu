package com.example.demo.model;

/** conversations：对应最终报告，数据访问只通过 Repository。 */
public class Conversation {
  public Integer conversation_id;
  public Integer initiator_id;
  public Integer peer_id;
  public String created_at;
}
