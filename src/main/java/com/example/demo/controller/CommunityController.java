package com.example.demo.controller;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.service.CommunityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommunityController {
  private final CommunityService service;

  public CommunityController(CommunityService service) {
    this.service = service;
  }

  @PostMapping("/conversations")
  public ResponseEntity<ConversationResponse> conversation(
      @RequestAttribute("userId") int uid, @RequestBody ConversationInput input) {
    return ResponseEntity.ok(service.conversation(uid, input));
  }

  @PostMapping("/conversations/{id}/messages")
  public ResponseEntity<MessageResponse> message(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody TextInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.send(uid, id, input));
  }

  @PostMapping("/products/{id}/reports")
  public ResponseEntity<ReportResponse> report(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody TextInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.report(uid, id, input));
  }
}
