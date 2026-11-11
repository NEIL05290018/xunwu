package com.example.demo.controller;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
  private final AccountService service;

  public AccountController(AccountService service) {
    this.service = service;
  }

  @GetMapping("/market")
  public ResponseEntity<Bootstrap> bootstrap(
      @RequestAttribute("userId") int uid,
      @RequestAttribute(value = "adminId", required = false) Integer aid) {
    return ResponseEntity.ok(service.bootstrap(uid, aid));
  }

  @PostMapping("/addresses")
  public ResponseEntity<AddressResponse> create(
      @RequestAttribute("userId") int uid, @RequestBody AddressInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.address(uid, null, input));
  }

  @PutMapping("/addresses/{id}")
  public ResponseEntity<AddressResponse> update(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody AddressInput input) {
    return ResponseEntity.ok(service.address(uid, id, input));
  }
}
