package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.mapper.*;
import com.example.demo.model.*;
import com.example.demo.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@ActiveProfiles("test")
@SpringBootTest
class MyBatisMappingTests extends DatabaseTestSupport {
  @Autowired ProductMapper products;
  @Autowired ConversationMapper conversations;
  @Autowired CommunityService community;
  @Autowired CatalogService catalog;
  @Autowired TradeService trades;
  @Autowired UserMapper users;
  @MockitoSpyBean ProductImageMapper images;

  @Test
  void dynamicSqlBindsValuesAndMapsFields() {
    Product p = products.findById(101);
    assertEquals("机械键盘", p.title);
    assertEquals(18000, p.price_cents);
    assertEquals(1, p.seller_id);
    assertFalse(products.search(1, "market", "数码配件", "机械").isEmpty());
    assertTrue(products.search(1, "market", null, "' OR 1=1 --").isEmpty());
    assertTrue(products.search(1, "market", null, "%").isEmpty());
    assertFalse(products.search(3, "mine", null, null).isEmpty());
    for (Product draft : products.search(1, "all", null, null))
      assertTrue(draft.seller_id == 1 || "通过".equals(draft.audit_status));
  }

  @Test
  void generatedKeyAndDatabaseImageFailureRollback() {
    int before = jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
    doThrow(new org.springframework.dao.DataIntegrityViolationException("模拟图片写入失败"))
        .when(images)
        .insert(any(ProductImage.class));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            catalog.save(
                1,
                null,
                new ProductInput(
                    "失败不残留", "测试事务", 1200, "生活用品", true, true, List.of("/assets/lamp.svg"))));
    assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class));
    assertEquals(
        0, jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE title='失败不残留'", Integer.class));
  }

  @Test
  void actualInitiatorAndConcurrentReverseConversation() throws Exception {
    var c = community.conversation(5, new ConversationInput(4));
    assertEquals(5, c.initiator_id());
    assertEquals(4, c.peer_id());
    assertEquals(
        c.conversation_id(), community.conversation(4, new ConversationInput(5)).conversation_id());
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Future<Integer> a =
          pool.submit(
              () -> {
                start.await();
                return community.conversation(5, new ConversationInput(1)).conversation_id();
              });
      Future<Integer> b =
          pool.submit(
              () -> {
                start.await();
                return community.conversation(1, new ConversationInput(5)).conversation_id();
              });
      start.countDown();
      assertEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(
          1,
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM conversations WHERE pair_low=1 AND pair_high=5",
              Integer.class));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void credentialsAreMappedButNeverSerialized() throws Exception {
    User u = users.findByPhone("19900000001");
    assertTrue(com.example.demo.security.Passwords.matches("TestUser_2026", u.password_hash));
    assertFalse(testMapper.writeValueAsString(u).contains("password_hash"));
  }

  @Test
  void selfPickupAndShippingDatabaseConstraints() {
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> jdbc.update("UPDATE orders SET shipping_fee_cents=1 WHERE order_id=201"));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> jdbc.update("UPDATE orders SET recipient_address=NULL WHERE order_id=204"));
  }

  @Test
  void pickupTradeCompletes() {
    var order = trades.create(4, new OrderInput(112, "自提", null));
    assertEquals(0, order.shipping_fee_cents());
    trades.transition(4, order.order_id(), "pay", null);
    int seller = products.findById(112).seller_id;
    assertEquals("已完成", trades.transition(seller, order.order_id(), "ship", null).order_status());
    assertEquals("已售", products.findById(112).product_status);
  }
}
