package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.mapper.*;
import com.example.demo.model.*;
import com.example.demo.service.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseMyBatisTests extends DatabaseTestSupport {
  @MockitoSpyBean ProductMapper products;
  @Autowired MarketQueryMapper queries;
  @Autowired CatalogService catalog;
  @Autowired MockMvc mvc;

  int create(int user) {
    return catalog
        .save(
            user,
            null,
            new ProductInput("课程测试商品", "描述", 1234, "生活用品", true, true, List.of("/assets/lamp.svg")))
        .product_id();
  }

  @Test
  void mapperCrudAndGeneratedKeys() {
    Product p =
        Product.builder()
            .seller_id(1)
            .title("CRUD测试")
            .description("说明")
            .price_cents(100)
            .category("生活用品")
            .support_express(1)
            .support_pickup(1)
            .product_status("草稿")
            .audit_status("待审核")
            .build();
    assertEquals(1, products.insert(p));
    assertNotNull(p.product_id);
    assertEquals("CRUD测试", products.findById(p.product_id).title);
    assertTrue(products.findAll().stream().anyMatch(x -> x.product_id.equals(p.product_id)));
    assertTrue(
        products.findByCategory("生活用品").stream().anyMatch(x -> x.product_id.equals(p.product_id)));
    p.title = "已修改";
    assertEquals(1, products.update(p));
    assertEquals("已修改", products.findById(p.product_id).title);
    assertEquals(1, products.deleteById(p.product_id));
    assertNull(products.findById(p.product_id));
  }

  @Test
  void combinedRangesAndInvalidInputs() throws Exception {
    assertTrue(
        products.searchRange(1, "market", "数码配件", "机械", 18000, 18000).stream()
            .anyMatch(p -> p.product_id == 101));
    assertTrue(products.searchRange(1, "market", "数码配件", "机械", 18001, null).isEmpty());
    assertTrue(products.searchRange(1, "market", null, "' OR 1=1 --", null, null).isEmpty());
    for (Product p : products.searchRange(1, "market", null, null, null, 5000))
      assertTrue(p.price_cents <= 5000);
    mvc.perform(get("/api/products?minPrice=200&maxPrice=100").session(userSession(1)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/products?minPrice=-1").session(userSession(1)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/products?minPrice=1.5").session(userSession(1)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/products?minPrice=18000&maxPrice=18000&q=机械").session(userSession(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].product_id").value(101));
  }

  @Test
  void batchDeletesChildrenAndDeduplicates() {
    int a = create(1), b = create(1);
    catalog.favorite(1, a, true);
    assertEquals(2, catalog.deleteBatch(1, List.of(b, a, a)));
    assertNull(products.findById(a));
    assertNull(products.findById(b));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM product_images WHERE product_id=?", Integer.class, a));
    assertEquals(
        0,
        jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE product_id=?", Integer.class, a));
    assertEquals(0, products.deleteBatch(List.of()));
    assertFalse(products.findAll().isEmpty());
  }

  @Test
  void batchRejectsUnauthorizedMissingHistoryAndEmpty() {
    int a = create(1), b = create(2);
    for (List<Integer> ids :
        List.of(List.of(a, b), List.of(a, 999999), List.of(a, 106), List.<Integer>of())) {
      assertThrows(BusinessException.class, () -> catalog.deleteBatch(1, ids));
      assertNotNull(products.findById(a));
    }
    assertThrows(BusinessException.class, () -> catalog.deleteBatch(1, Arrays.asList(a, null)));
    assertThrows(
        BusinessException.class, () -> catalog.deleteBatch(1, Collections.nCopies(101, a)));
  }

  @Test
  void batchSqlFailureRollsBackDeletedChildren() {
    int a = create(1);
    catalog.favorite(1, a, true);
    doThrow(new org.springframework.dao.DataIntegrityViolationException("模拟最后一条SQL失败"))
        .when(products)
        .deleteBatch(anyList());
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> catalog.deleteBatch(1, List.of(a)));
    assertNotNull(products.findById(a));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM product_images WHERE product_id=?", Integer.class, a));
    assertEquals(
        1,
        jdbc.queryForObject("SELECT COUNT(*) FROM favorites WHERE product_id=?", Integer.class, a));
  }

  @Test
  void joinsRespectIdentityVisibilityAndSnapshot() throws Exception {
    var p = queries.detail(2, 101);
    assertEquals(1, p.seller_id);
    assertNotNull(p.seller_nickname);
    int draft = create(1);
    assertNull(queries.detail(2, draft));
    assertNotNull(queries.detail(1, draft));
    catalog.favorite(1, draft, true);
    assertTrue(queries.favorites(1).stream().anyMatch(x -> x.product_id == draft));
    assertTrue(queries.favorites(2).stream().noneMatch(x -> x.product_id == draft));
    for (var o : queries.purchases(2)) {
      assertEquals(2, o.buyer_id);
      assertNotNull(o.seller_nickname);
    }
    int buyer =
        jdbc.queryForObject("SELECT buyer_id FROM orders WHERE order_id=201", Integer.class);
    String title =
        jdbc.queryForObject("SELECT snapshot_title FROM orders WHERE order_id=201", String.class);
    jdbc.update(
        "UPDATE products SET title='现名' WHERE product_id=(SELECT product_id FROM orders WHERE"
            + " order_id=201)");
    assertEquals(
        title,
        queries.purchases(buyer).stream()
            .filter(o -> o.order_id == 201)
            .findFirst()
            .orElseThrow()
            .snapshot_title);
    mvc.perform(get("/api/queries/purchases")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/queries/products/" + draft).session(userSession(2)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/queries/products/101").session(userSession(2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password_hash").doesNotExist())
        .andExpect(jsonPath("$.phone").doesNotExist());
  }

  @Test
  void batchHttpRequiresCsrfAndUsesSessionOwner() throws Exception {
    int a = create(1);
    mvc.perform(
            post("/api/products/batch-delete")
                .session(userSession(1))
                .contentType("application/json")
                .content("{\"ids\":[" + a + "]}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/products/batch-delete")
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"ids\":[" + a + "],\"uid\":1}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/products/batch-delete")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"ids\":[" + a + "]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(1));
  }
}
