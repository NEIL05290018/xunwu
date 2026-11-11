package com.example.demo.controller;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.service.CatalogService;
import com.example.demo.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 课程演示身份，不是正式登录鉴权。 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final CatalogService catalog;
  private final CommunityService community;

  public AdminController(CatalogService catalog, CommunityService community) {
    this.catalog = catalog;
    this.community = community;
  }

  @PutMapping("/products/{id}/audit")
  public ResponseEntity<ProductResponse> audit(
      @RequestAttribute("adminId") int aid,
      @PathVariable int id,
      @RequestBody DecisionInput input) {
    return ResponseEntity.ok(catalog.audit(aid, id, input));
  }

  @PutMapping("/reports/{id}")
  public ResponseEntity<ReportResponse> report(
      @RequestAttribute("adminId") int aid,
      @PathVariable int id,
      @RequestBody DecisionInput input) {
    return ResponseEntity.ok(community.decideReport(aid, id, input));
  }
}
