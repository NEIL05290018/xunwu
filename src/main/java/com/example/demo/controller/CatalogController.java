package com.example.demo.controller;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.service.BusinessException;
import com.example.demo.service.CatalogService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class CatalogController {
  private final CatalogService service;

  public CatalogController(CatalogService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<ProductResponse>> find(
      @RequestAttribute("userId") int uid,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "market") String scope,
      @RequestParam(required = false) Integer minPrice,
      @RequestParam(required = false) Integer maxPrice) {
    return ResponseEntity.ok(service.find(uid, category, q, scope, minPrice, maxPrice));
  }

  public record BatchDeleteInput(List<Integer> ids) {}

  @PostMapping("/batch-delete")
  public java.util.Map<String, Integer> deleteBatch(
      @RequestAttribute("userId") int uid, @RequestBody BatchDeleteInput input) {
    return java.util.Map.of("deleted", service.deleteBatch(uid, input.ids()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> detail(
      @RequestAttribute("userId") int uid, @PathVariable int id) {
    try {
      return ResponseEntity.ok(service.detail(uid, id));
    } catch (BusinessException e) {
      if (e.kind == BusinessException.Kind.NOT_FOUND) return ResponseEntity.notFound().build();
      throw e;
    }
  }

  @PostMapping
  public ResponseEntity<ProductResponse> create(
      @RequestAttribute("userId") int uid, @RequestBody ProductInput input) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.save(uid, null, input));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @RequestAttribute("userId") int uid, @PathVariable int id, @RequestBody ProductInput input) {
    try {
      return ResponseEntity.ok(service.save(uid, id, input));
    } catch (BusinessException e) {
      if (e.kind == BusinessException.Kind.NOT_FOUND) return ResponseEntity.notFound().build();
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@RequestAttribute("userId") int uid, @PathVariable int id) {
    try {
      service.delete(uid, id);
      return ResponseEntity.noContent().build();
    } catch (BusinessException e) {
      if (e.kind == BusinessException.Kind.NOT_FOUND) return ResponseEntity.notFound().build();
      throw e;
    }
  }

  @PutMapping("/{id}/favorite")
  public ResponseEntity<Void> favorite(@RequestAttribute("userId") int uid, @PathVariable int id) {
    service.favorite(uid, id, true);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/favorite")
  public ResponseEntity<Void> unfavorite(
      @RequestAttribute("userId") int uid, @PathVariable int id) {
    service.favorite(uid, id, false);
    return ResponseEntity.noContent().build();
  }
}
