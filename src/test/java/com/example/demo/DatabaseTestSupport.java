package com.example.demo;

import com.example.demo.security.Passwords;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

/** 所有测试只连接独立的 xunwu_test 数据库，每项测试恢复夹具。 */
abstract class DatabaseTestSupport {
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper testMapper;
  private static final String[] TABLES = {
    "admins",
    "users",
    "addresses",
    "products",
    "product_images",
    "orders",
    "conversations",
    "messages",
    "refunds",
    "reviews",
    "reports",
    "favorites"
  };

  @BeforeEach
  void resetDatabase() throws Exception {
    String db = jdbc.queryForObject("SELECT DATABASE()", String.class);
    if (db == null || !db.endsWith("_test"))
      throw new IllegalStateException("测试数据库名称必须以 _test 结尾，拒绝清空其他库");
    for (int i = TABLES.length - 1; i >= 0; i--) jdbc.update("DELETE FROM `" + TABLES[i] + "`");
    JsonNode seed = testMapper.readTree(new ClassPathResource("demo-seed.json").getInputStream());
    for (String table : TABLES)
      for (JsonNode row : seed.get(table)) {
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        row.fields()
            .forEachRemaining(
                entry -> {
                  columns.add(entry.getKey());
                  values.add(testMapper.convertValue(entry.getValue(), Object.class));
                });
        if (table.equals("users") || table.equals("admins")) {
          columns.add("password_hash");
          values.add("test-placeholder");
        }
        jdbc.update(
            "INSERT INTO `"
                + table
                + "` ("
                + String.join(",", columns)
                + ") VALUES ("
                + String.join(",", Collections.nCopies(columns.size(), "?"))
                + ")",
            values.toArray());
      }
    jdbc.update("UPDATE users SET password_hash=?", Passwords.hash("TestUser_2026"));
    jdbc.update("UPDATE admins SET password_hash=?", Passwords.hash("TestAdmin_2026"));
  }

  protected MockHttpSession userSession(int id) {
    MockHttpSession s = new MockHttpSession();
    s.setAttribute("role", "USER");
    s.setAttribute("principalId", id);
    s.setAttribute("nickname", "测试用户");
    s.setAttribute("csrf", "test-csrf");
    return s;
  }

  protected MockHttpSession adminSession(int id) {
    MockHttpSession s = userSession(id);
    s.setAttribute("role", "ADMIN");
    return s;
  }
}
