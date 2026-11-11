package com.example.demo.controller;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.service.AccountService;
import com.example.demo.service.TradeService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TradeController {
  private final TradeService service;
  private final AccountService accounts;

  public TradeController(TradeService service, AccountService accounts) {
    this.service = service;
    this.accounts = accounts;
  }

  @GetMapping("/orders")
  public ResponseEntity<List<TradeOrderResponse>> orders(@RequestAttribute("userId") int uid) {
    return ResponseEntity.ok(accounts.bootstrap(uid, null).orders());
  }

  @PostMapping("/orders")
  public ResponseEntity<TradeOrderResponse> create(
      @RequestAttribute("userId") int uid, @RequestBody OrderInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(uid, input));
  }

  @PostMapping("/orders/{id}/actions/{action}")
  public ResponseEntity<TradeOrderResponse> transition(
      @RequestAttribute("userId") int uid,
      @PathVariable int id,
      @PathVariable String action,
      @RequestBody(required = false) ShippingInput input) {
    return ResponseEntity.ok(service.transition(uid, id, action, input));
  }

  @PostMapping("/orders/{id}/refunds")
  public ResponseEntity<RefundResponse> refund(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody TextInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.refund(uid, id, input));
  }

  @PutMapping("/refunds/{id}")
  public ResponseEntity<RefundResponse> decideRefund(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody DecisionInput input) {
    return ResponseEntity.ok(service.decideRefund(uid, id, input));
  }

  @PostMapping("/orders/{id}/reviews")
  public ResponseEntity<ReviewResponse> review(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody TextInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.review(uid, id, input));
  }
}
