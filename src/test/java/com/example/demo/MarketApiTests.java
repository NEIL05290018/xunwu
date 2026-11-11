package com.example.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.demo.dto.MarketRequests.OrderInput;
import com.example.demo.repository.MarketRepository;
import com.example.demo.service.BusinessException;
import com.example.demo.service.TradeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MarketApiTests extends DatabaseTestSupport {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired TradeService trades;
  @Autowired MarketRepository repository;

  private JsonNode body(MvcResult r) throws Exception {
    return mapper.readTree(r.getResponse().getContentAsString());
  }

  private String productInput() {
    return """
{"title":"工程化测试商品","description":"九成新，功能正常","price_cents":18000,"category":"数码设备",
 "support_express":true,"support_pickup":true,"images":["/assets/camera.svg"],"seller_id":99,"product_status":"已售"}
""";
  }

  @Test
  void completeTradingLifecycleAndSnapshots() throws Exception {
    int product =
        body(mvc.perform(
                    post("/api/products")
                        .session(userSession(2))
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType("application/json")
                        .content(productInput()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seller_id").value(2))
                .andExpect(jsonPath("$.product_status").value("草稿"))
                .andReturn())
            .get("product_id")
            .asInt();
    mvc.perform(
            put("/api/admin/products/" + product + "/audit")
                .session(adminSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"agree\":true,\"note\":\"通过\"}"))
        .andExpect(status().isOk());
    String orderInput =
        "{\"product_id\":" + product + ",\"delivery_method\":\"快递\",\"address_id\":1}";
    int order =
        body(mvc.perform(
                    post("/api/orders")
                        .session(userSession(1))
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType("application/json")
                        .content(orderInput))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shipping_fee_cents").value(600))
                .andReturn())
            .get("order_id")
            .asInt();
    mvc.perform(
            post("/api/orders")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(orderInput))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/addresses/1")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(
                    "{\"recipient_name\":\"新收件人\",\"recipient_phone\":\"19900000001\",\"detail_address\":\"新的示例地址\"}"))
        .andExpect(status().isOk());
    assertNotEquals(
        "新的示例地址",
        repository.snapshot().orders.stream()
            .filter(o -> o.order_id == order)
            .findFirst()
            .orElseThrow()
            .recipient_address);
    mvc.perform(
            post("/api/orders/" + order + "/actions/pay")
                .session(userSession(3))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/orders/" + order + "/actions/pay")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/orders/" + order + "/actions/ship")
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"carrier\":\"演示快递\",\"tracking_no\":\"DEMO-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order_status").value("待收货"));
    mvc.perform(
            post("/api/orders/" + order + "/actions/receive")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/orders/" + order + "/reviews")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"content\":\"沟通顺利\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/orders/" + order + "/reviews")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"content\":\"重复评价\"}"))
        .andExpect(status().isConflict());
    int refund =
        body(mvc.perform(
                    post("/api/orders/" + order + "/refunds")
                        .session(userSession(1))
                        .header("X-CSRF-Token", "test-csrf")
                        .contentType("application/json")
                        .content("{\"content\":\"协商退款\"}"))
                .andExpect(status().isCreated())
                .andReturn())
            .get("refund_id")
            .asInt();
    mvc.perform(
            put("/api/refunds/" + refund)
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"agree\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refund_status").value("退款成功"));
    assertEquals(
        "已退款",
        repository.snapshot().orders.stream()
            .filter(o -> o.order_id == order)
            .findFirst()
            .orElseThrow()
            .order_status);
  }

  @Test
  void rejectsSelfPurchaseInvalidRequestsAndUnauthorizedEdits() throws Exception {
    mvc.perform(
            post("/api/orders")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"product_id\":101,\"delivery_method\":\"自提\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/products/101")
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(productInput()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/products")
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(productInput().replace("18000", "-1")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/products")
                .session(userSession(2))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content(productInput().replace("18000", "12.5")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/products/99999").session(userSession(1)).header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isNotFound());
    mvc.perform(
            put("/api/admin/products/111/audit")
                .session(adminSession(999))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"agree\":true,\"note\":\"通过\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/students").session(userSession(1))).andExpect(status().isNotFound());
  }

  @Test
  void scopesPrivateDataAndDeduplicatesConversationsAndFavorites() throws Exception {
    JsonNode state =
        body(
            mvc.perform(
                    get("/api/market").session(userSession(4)).header("X-CSRF-Token", "test-csrf"))
                .andExpect(status().isOk())
                .andReturn());
    assertEquals(12, state.size());
    assertEquals(0, state.get("conversations").size());
    assertEquals(0, state.get("messages").size());
    assertEquals(0, state.get("addresses").size());
    mvc.perform(
            post("/api/conversations/301/messages")
                .session(userSession(4))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"content\":\"越权发送\"}"))
        .andExpect(status().isForbidden());
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/conversations")
                  .session(userSession(1))
                  .header("X-CSRF-Token", "test-csrf")
                  .contentType("application/json")
                  .content("{\"peer_id\":2}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.conversation_id").value(301));
    for (int i = 0; i < 2; i++)
      mvc.perform(
              put("/api/products/107/favorite")
                  .session(userSession(1))
                  .header("X-CSRF-Token", "test-csrf"))
          .andExpect(status().isNoContent())
          .andExpect(content().string(""));
    assertEquals(
        1,
        repository.snapshot().favorites.stream()
            .filter(f -> f.user_id == 1 && f.product_id == 107)
            .count());
    mvc.perform(
            delete("/api/products/107/favorite")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isNoContent());
  }

  @Test
  void failedShippingRollsBackAndRefundBlocksReceiving() throws Exception {
    trades.transition(2, 202, "pay", null);
    mvc.perform(
            post("/api/orders/202/actions/ship")
                .session(userSession(3))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"carrier\":\"不应保存\",\"tracking_no\":\" \"}"))
        .andExpect(status().isBadRequest());
    assertNull(
        repository.snapshot().orders.stream()
            .filter(o -> o.order_id == 202)
            .findFirst()
            .orElseThrow()
            .carrier);
    mvc.perform(
            post("/api/orders/204/refunds")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf")
                .contentType("application/json")
                .content("{\"content\":\"申请退款\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/orders/204/actions/receive")
                .session(userSession(1))
                .header("X-CSRF-Token", "test-csrf"))
        .andExpect(status().isConflict());
  }

  @Test
  void concurrentBuyersCannotReserveSameProduct() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      Callable<Boolean> first =
          () -> {
            start.await();
            try {
              trades.create(4, new OrderInput(112, "自提", null));
              return true;
            } catch (BusinessException e) {
              assertEquals(BusinessException.Kind.CONFLICT, e.kind);
              return false;
            }
          };
      Callable<Boolean> second =
          () -> {
            start.await();
            try {
              trades.create(5, new OrderInput(112, "自提", null));
              return true;
            } catch (BusinessException e) {
              assertEquals(BusinessException.Kind.CONFLICT, e.kind);
              return false;
            }
          };
      Future<Boolean> a = pool.submit(first), b = pool.submit(second);
      start.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
      assertEquals(
          1, repository.snapshot().orders.stream().filter(o -> o.product_id == 112).count());
    } finally {
      pool.shutdownNow();
    }
  }
}
