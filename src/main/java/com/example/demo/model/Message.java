package com.example.demo.model;

/** messages：对应最终报告，数据访问只通过 Repository。 */
public class Message {
  public Integer message_id;
  public Integer conversation_id;
  public Integer sender_id;
  public String content;
  public String sent_at;
}
