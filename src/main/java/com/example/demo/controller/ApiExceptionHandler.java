package com.example.demo.controller;

import com.example.demo.dto.MarketResponses.ErrorResponse;
import com.example.demo.service.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> business(BusinessException error) {
    int code =
        switch (error.kind) {
          case INVALID -> 400;
          case NOT_FOUND -> 404;
          case FORBIDDEN -> 403;
          case CONFLICT -> 409;
        };
    return ResponseEntity.status(code)
        .body(new ErrorResponse(error.kind.name(), error.getMessage()));
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> integrity(Exception error) {
    return ResponseEntity.status(409).body(new ErrorResponse("CONFLICT", "操作与已有数据冲突，请刷新后重试"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> malformed(HttpMessageNotReadableException error) {
    return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_JSON", "请求 JSON 类型或格式有误"));
  }
}
