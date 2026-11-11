package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.demo.model.Product;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthAndPersistenceTests extends DatabaseTestSupport {
  @Autowired MockMvc mvc;
  @Autowired MarketRepository market;
  @Autowired ProductRepository products;
  private static final String INPUT =
      """
{"title":"数据库测试商品","description":"功能正常，有轻微使用痕迹","price_cents":4500,"category":"生活用品","support_express":true,"support_pickup":true,"images":["/assets/lamp.svg"]}
""";

  @Test
  void registrationLoginLogoutAndIdentitySpoofing() throws Exception {
    mvc.perform(get("/api/market").header("X-Demo-User", 1).header("X-Demo-Admin", 1))
        .andExpect(status().isUnauthorized());
    var initial = mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andReturn();
    MockHttpSession guest = (MockHttpSession) initial.getRequest().getSession();
    String csrf =
        testMapper.readTree(initial.getResponse().getContentAsString()).get("csrf").asText();
    String registration =
        "{\"nickname\":\"新同学\",\"phone\":\"18812345678\",\"password\":\"SecurePass_2026\"}";
    mvc.perform(
            post("/api/auth/register")
                .session(guest)
                .contentType("application/json")
                .content(registration))
        .andExpect(status().isForbidden());
    var registered =
        mvc.perform(
                post("/api/auth/register")
                    .session(guest)
                    .header("X-CSRF-Token", csrf)
                    .contentType("application/json")
                    .content(registration))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("USER"))
            .andReturn();
    MockHttpSession session = (MockHttpSession) registered.getRequest().getSession();
    String token =
        testMapper.readTree(registered.getResponse().getContentAsString()).get("csrf").asText();
    String hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM users WHERE phone='18812345678'", String.class);
    assertFalse(hash.contains("SecurePass"));
    mvc.perform(get("/api/market").session(session).header("X-Demo-Admin", 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.admins").isEmpty())
        .andExpect(jsonPath("$.addresses").isEmpty());
    mvc.perform(
            put("/api/admin/products/111/audit")
                .session(session)
                .header("X-CSRF-Token", token)
                .header("X-Demo-Admin", 1)
                .contentType("application/json")
                .content("{\"agree\":true,\"note\":\"通过\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/auth/register")
                .session(session)
                .header("X-CSRF-Token", token)
                .contentType("application/json")
                .content(registration))
        .andExpect(status().isConflict());
    mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-Token", token))
        .andExpect(status().isNoContent());
    var fresh = mvc.perform(get("/api/auth/session")).andReturn();
    MockHttpSession loginSession = (MockHttpSession) fresh.getRequest().getSession();
    String loginToken =
        testMapper.readTree(fresh.getResponse().getContentAsString()).get("csrf").asText();
    mvc.perform(
            post("/api/auth/login")
                .session(loginSession)
                .header("X-CSRF-Token", loginToken)
                .contentType("application/json")
                .content("{\"account\":\"18812345678\",\"password\":\"wrong_password\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .session(loginSession)
                .header("X-CSRF-Token", loginToken)
                .contentType("application/json")
                .content("{\"account\":\"18812345678\",\"password\":\"SecurePass_2026\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("新同学"));
  }

  @Test
  void productCrudPersistsAndProtectsHistory() throws Exception {
    var response =
        mvc.perform(
                post("/api/products")
                    .session(userSession(1))
                    .header("X-CSRF-Token", "test-csrf")
                    .contentType("application/json")
                    .content(INPUT))
            .andExpect(status().isCreated())
            .andReturn();
    int id =
        testMapper.readTree(response.getResponse().getContentAsString()).get("product_id").asInt();
    assertEquals(
        "数据库测试商品",
        jdbc.queryForObject("SELECT title FROM products WHERE product_id=?", String.class, id));
    mvc.perform(get("/api/products?scope=mine").session(userSession(1))).andExpect(status().isOk());
    mvc.perform(get("/api/products/" + id).session(userSession(2)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/products/" + id)
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(INPUT.replace("4500", "4600")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.price_cents").value(4600));
    mvc.perform(
            delete("/api/products/" + id)
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/products/" + id + "/favorite")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isNoContent());
    mvc.perform(
            delete("/api/products/" + id)
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isNoContent());
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM product_images WHERE product_id=?", Integer.class, id));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM favorites WHERE product_id=?", Integer.class, id));
    mvc.perform(get("/api/products/" + id).session(userSession(1)))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""));
    mvc.perform(
            put("/api/products/" + id)
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(INPUT))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""));
    mvc.perform(
            delete("/api/products/" + id)
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(""));
    assertTrue(market.allocateProductId() > id);
    int historical = jdbc.queryForObject("SELECT product_id FROM orders LIMIT 1", Integer.class);
    int seller =
        jdbc.queryForObject(
            "SELECT seller_id FROM products WHERE product_id=?", Integer.class, historical);
    mvc.perform(
            delete("/api/products/" + historical)
                .session(userSession(seller))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isConflict());
  }

  @Test
  void databaseConstraintsAndRollbackAreReal() {
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO favorites(user_id,product_id,favorited_at)"
                    + " VALUES(999999,101,'2026-11-11 12:00:00')"));
    assertThrows(
        IllegalStateException.class,
        () ->
            market.transaction(
                s -> {
                  jdbc.update("UPDATE products SET title='不应保存' WHERE product_id=101");
                  throw new IllegalStateException("rollback");
                }));
    assertNotEquals("不应保存", products.findById(101).title);
    assertEquals(
        12,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()",
            Integer.class));
  }

  @Test
  void builderAndRepositorySixMethods() {
    Product.Builder builder =
        Product.builder()
            .seller_id(1)
            .title("台灯")
            .description("无破损")
            .price_cents(100)
            .category("生活用品")
            .support_express(1)
            .support_pickup(1)
            .product_status("草稿")
            .audit_status("待审核");
    Product a = builder.build(), b = builder.build();
    a.title = "修改";
    assertEquals("台灯", b.title);
    Product saved = products.save(b);
    assertNotNull(products.findById(saved.product_id));
    assertFalse(products.findAll().isEmpty());
    assertFalse(products.findByCategory("生活用品").isEmpty());
    assertTrue(products.findByCategory("不存在").isEmpty());
    saved.title = "更新";
    assertEquals("更新", products.update(saved.product_id, saved).title);
    assertTrue(products.deleteById(saved.product_id));
    assertFalse(products.deleteById(saved.product_id));
    assertNull(products.findById(saved.product_id));
  }

  @Test
  void queryScopesExcludeOtherDrafts() throws Exception {
    int draft =
        testMapper
            .readTree(
                mvc.perform(
                        post("/api/products")
                            .session(userSession(2))
                            .header("X-CSRF-Token", "test-csrf")
                            .contentType("application/json")
                            .content(INPUT))
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("product_id")
            .asInt();
    for (String scope : List.of("market", "mine", "all")) {
      JsonNode rows =
          testMapper.readTree(
              mvc.perform(get("/api/products?scope=" + scope).session(userSession(1)))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      for (JsonNode row : rows) {
        assertNotEquals(draft, row.get("product_id").asInt());
        if (scope.equals("mine")) assertEquals(1, row.get("seller_id").asInt());
      }
    }
    mvc.perform(get("/api/products?category=不存在").session(userSession(1)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(get("/api/products?scope=invalid").session(userSession(1)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pickupCancelAndReportLifecycle() throws Exception {
    int order =
        testMapper
            .readTree(
                mvc.perform(
                        post("/api/orders")
                            .session(userSession(4))
                            .header("X-CSRF-Token", "test-csrf")
                            .contentType("application/json")
                            .content("{\"product_id\":112,\"delivery_method\":\"自提\"}"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("order_id")
            .asInt();
    mvc.perform(
            post("/api/orders/" + order + "/actions/cancel")
                .session(userSession(4))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isOk());
    assertEquals("在售", products.findById(112).product_status);
    int report =
        testMapper
            .readTree(
                mvc.perform(
                        post("/api/products/112/reports")
                            .session(userSession(4))
                            .header("X-CSRF-Token", "test-csrf")
                            .contentType("application/json")
                            .content("{\"content\":\"描述不符\"}"))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("report_id")
            .asInt();
    mvc.perform(
            put("/api/admin/reports/" + report)
                .session(adminSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"agree\":true,\"note\":\"核实后下架\"}"))
        .andExpect(status().isOk());
    assertEquals("下架", products.findById(112).product_status);
    mvc.perform(
            post("/api/orders")
                .session(userSession(4))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"product_id\":112,\"delivery_method\":\"自提\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void conversationMessagesPersistAndStayPrivate() throws Exception {
    int conversation =
        testMapper
            .readTree(
                mvc.perform(
                        post("/api/conversations")
                            .session(userSession(5))
                            .header("X-CSRF-Token", "test-csrf")
                            .contentType("application/json")
                            .content("{\"peer_id\":4}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("conversation_id")
            .asInt();
    mvc.perform(
            post("/api/conversations/" + conversation + "/messages")
                .session(userSession(5))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"content\":\"聊天持久化测试\"}"))
        .andExpect(status().isCreated());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM messages WHERE content='聊天持久化测试'", Integer.class));
    String other =
        mvc.perform(get("/api/market").session(userSession(1)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(other.contains("聊天持久化测试"));
  }
}
