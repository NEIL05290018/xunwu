package com.example.demo.service;

/** 业务层不绑定 HTTP；由表现层把原因转换为相应状态码。 */
public class BusinessException extends RuntimeException {
  public enum Kind {
    INVALID,
    NOT_FOUND,
    FORBIDDEN,
    CONFLICT
  }

  public final Kind kind;

  public BusinessException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }
}
