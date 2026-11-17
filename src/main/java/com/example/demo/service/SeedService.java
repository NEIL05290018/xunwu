package com.example.demo.service;

import com.example.demo.mapper.SchemaMapper;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.example.demo.security.Passwords;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedService {
  private final MarketRepository db;
  private final SchemaMapper schema;
  private final ObjectMapper json;
  private final boolean demo;
  private final String adminPassword, userPassword;

  public SeedService(
      MarketRepository db,
      SchemaMapper schema,
      ObjectMapper json,
      @Value("${xunwu.seed-demo:false}") boolean demo,
      @Value("${xunwu.admin-password:}") String adminPassword,
      @Value("${xunwu.demo-password:}") String userPassword) {
    this.db = db;
    this.schema = schema;
    this.json = json;
    this.demo = demo;
    this.adminPassword = adminPassword;
    this.userPassword = userPassword;
  }

  @Transactional(rollbackFor = Exception.class)
  public void initialize() throws Exception {
    if (schema.conversationVersion() != 2)
      throw new IllegalStateException(
          "检测到旧数据库结构，请先备份并执行 database/migrate-v3-to-v4.sql，或新建空数据库。不会自动清空旧数据。");
    if (schema.adminCount() > 0) return;
    if (schema.userCount() > 0) throw new IllegalStateException("数据库有用户却没有管理员，拒绝覆盖，请检查初始化状态。");
    Passwords.validate(adminPassword);
    if (demo) Passwords.validate(userPassword);
    MarketState seed;
    try (var input = new ClassPathResource("demo-seed.json").getInputStream()) {
      seed = demo ? json.readValue(input, MarketState.class) : new MarketState();
    }
    if (!demo) {
      Admin admin = new Admin();
      admin.admin_id = 1;
      admin.login_name = "admin01";
      seed.admins.add(admin);
    }
    for (Admin admin : seed.admins) admin.password_hash = Passwords.hash(adminPassword);
    for (User user : seed.users) user.password_hash = Passwords.hash(userPassword);
    for (Admin value : seed.admins) db.admin().insert(value);
    for (User value : seed.users) db.user().insert(value);
    for (Address value : seed.addresses) db.address().insert(value);
    for (Product value : seed.products) db.product().insert(value);
    for (ProductImage value : seed.product_images) db.productImage().insert(value);
    for (TradeOrder value : seed.orders) db.tradeOrder().insert(value);
    for (Conversation value : seed.conversations) db.conversation().insert(value);
    for (Message value : seed.messages) db.message().insert(value);
    for (Refund value : seed.refunds) db.refund().insert(value);
    for (Review value : seed.reviews) db.review().insert(value);
    for (Report value : seed.reports) db.report().insert(value);
    for (Favorite value : seed.favorites) db.favorite().insert(value);
  }
}
