package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.MarketQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queries")
public class MarketQueryController {
  private final MarketQueryService service;

  public MarketQueryController(MarketQueryService service) {
    this.service = service;
  }

  @GetMapping("/products/{id}")
  public JoinedProduct detail(@RequestAttribute("userId") int uid, @PathVariable int id) {
    return service.detail(uid, id);
  }

  @GetMapping("/favorites")
  public List<JoinedProduct> favorites(@RequestAttribute("userId") int uid) {
    return service.favorites(uid);
  }

  @GetMapping("/purchases")
  public List<PurchaseSummary> purchases(@RequestAttribute("userId") int uid) {
    return service.purchases(uid);
  }
}
