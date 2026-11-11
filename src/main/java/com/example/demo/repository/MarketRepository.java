package com.example.demo.repository;

import com.example.demo.model.*;
import com.example.demo.security.Passwords;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC 持久层。每次操作读取数据库；提交差异行，异常由 MySQL 事务回滚。 课程版以管理员首行作为跨进程写锁，保证“查可售→锁商品→建订单”原子执行。
 * 不适合高吞吐系统，但不再依赖服务器内存保存业务数据。
 */
@Repository
@DependsOnDatabaseInitialization
public class MarketRepository {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final ObjectMapper mapper;

  private record Table(String name, Class<?> type, String columns, String keys) {}

  private static final Table[] TABLES = {
    new Table("admins", Admin.class, "admin_id,login_name", "admin_id"),
    new Table("users", User.class, "user_id,nickname,phone,account_status", "user_id"),
    new Table(
        "addresses",
        Address.class,
        "address_id,user_id,recipient_name,recipient_phone,detail_address",
        "address_id"),
    new Table(
        "products",
        Product.class,
        "product_id,seller_id,title,description,price_cents,category,support_express,support_pickup,product_status,audit_status,audit_admin_id,audit_note",
        "product_id"),
    new Table("product_images", ProductImage.class, "image_id,product_id,image_url", "image_id"),
    new Table(
        "orders",
        TradeOrder.class,
        "order_id,product_id,buyer_id,snapshot_title,deal_price_cents,shipping_fee_cents,delivery_method,recipient_name,recipient_phone,recipient_address,order_status,carrier,tracking_no,created_at",
        "order_id"),
    new Table(
        "conversations",
        Conversation.class,
        "conversation_id,initiator_id,peer_id,created_at",
        "conversation_id"),
    new Table(
        "messages",
        Message.class,
        "message_id,conversation_id,sender_id,content,sent_at",
        "message_id"),
    new Table(
        "refunds",
        Refund.class,
        "refund_id,order_id,reason,refund_status,admin_id,admin_opinion,created_at",
        "refund_id"),
    new Table(
        "reviews", Review.class, "review_id,order_id,reviewer_id,content,created_at", "review_id"),
    new Table(
        "reports",
        Report.class,
        "report_id,reporter_id,product_id,reason,report_status,admin_id,handle_note,created_at",
        "report_id"),
    new Table("favorites", Favorite.class, "user_id,product_id,favorited_at", "user_id,product_id"),
  };

  public MarketRepository(
      JdbcTemplate jdbc,
      TransactionTemplate transactions,
      ObjectMapper mapper,
      @Value("${xunwu.seed-demo:false}") boolean demo,
      @Value("${xunwu.admin-password:}") String adminPassword,
      @Value("${xunwu.demo-password:}") String demoPassword)
      throws IOException {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.mapper = mapper;
    if (jdbc.queryForObject("SELECT COUNT(*) FROM admins", Integer.class) == 0) {
      Passwords.validate(adminPassword);
      if (demo) Passwords.validate(demoPassword);
      MarketState seed;
      try (var stream = new ClassPathResource("demo-seed.json").getInputStream()) {
        seed = demo ? mapper.readValue(stream, MarketState.class) : new MarketState();
      }
      if (!demo) {
        Admin a = new Admin();
        a.admin_id = 1;
        a.login_name = "admin01";
        seed.admins.add(a);
      }
      transactions.execute(
          new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
              persist(new MarketState(), seed);
              jdbc.update("UPDATE admins SET password_hash=?", Passwords.hash(adminPassword));
              if (demo)
                jdbc.update("UPDATE users SET password_hash=?", Passwords.hash(demoPassword));
              return null;
            }
          });
    }
  }

  public interface Operation<T> {
    T apply(MarketState working);
  }

  public MarketState snapshot() {
    return transactions.execute(
        new TransactionCallback<MarketState>() {
          @Override
          public MarketState doInTransaction(TransactionStatus status) {
            return load();
          }
        });
  }

  public <T> T transaction(Operation<T> operation) {
    return transactions.execute(
        new TransactionCallback<T>() {
          @Override
          public T doInTransaction(TransactionStatus status) {
            // 所有写路径使用相同数据库行锁，锁在提交或回滚后释放。
            jdbc.queryForList("SELECT admin_id FROM admins ORDER BY admin_id LIMIT 1 FOR UPDATE");
            MarketState before = load();
            MarketState working = mapper.convertValue(before, MarketState.class);
            T result = operation.apply(working);
            persist(before, working);
            return result;
          }
        });
  }

  public int allocateProductId() {
    // AUTO_INCREMENT 高水位在删除后仍保留；实际插入在当前事务提交阶段完成。
    Number next =
        (Number) jdbc.queryForMap("SHOW TABLE STATUS LIKE 'products'").get("Auto_increment");
    int maximum =
        jdbc.queryForObject("SELECT COALESCE(MAX(product_id),0) FROM products", Integer.class);
    return Math.max(maximum + 1, next == null ? 1 : next.intValue());
  }

  @SuppressWarnings("unchecked")
  private List<Object> rows(MarketState state, Table table) {
    try {
      return (List<Object>) MarketState.class.getField(table.name).get(state);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private Object field(Object row, String column) {
    try {
      return row.getClass().getField(column).get(row);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private String key(Object row, Table t) {
    StringBuilder key = new StringBuilder();
    for (String column : t.keys.split(",")) key.append(field(row, column)).append(":");
    return key.toString();
  }

  private Map<String, Object> index(List<Object> rows, Table t) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Object row : rows) result.put(key(row, t), row);
    return result;
  }

  private MarketState load() {
    MarketState state = new MarketState();
    for (Table t : TABLES) {
      for (Map<String, Object> record :
          jdbc.queryForList("SELECT " + t.columns + " FROM `" + t.name + "` ORDER BY " + t.keys))
        rows(state, t).add(mapper.convertValue(record, t.type));
    }
    return state;
  }

  private String where(Table t) {
    List<String> parts = new ArrayList<>();
    for (String k : t.keys.split(",")) parts.add(k + "=?");
    return String.join(" AND ", parts);
  }

  private Object[] values(Object row, String columns) {
    List<Object> values = new ArrayList<>();
    for (String c : columns.split(",")) values.add(field(row, c));
    return values.toArray();
  }

  private void persist(MarketState before, MarketState after) {
    // 参数化 SQL；表名和字段仅来自上述固定白名单，不接受客户端 SQL。
    for (Table t : TABLES) {
      Map<String, Object> old = index(rows(before, t), t);
      for (Object row : rows(after, t)) {
        Object previous = old.get(key(row, t));
        if (previous == null) {
          String placeholders =
              String.join(",", Collections.nCopies(t.columns.split(",").length, "?"));
          jdbc.update(
              "INSERT INTO `" + t.name + "` (" + t.columns + ") VALUES (" + placeholders + ")",
              values(row, t.columns));
        } else if (!mapper.valueToTree(previous).equals(mapper.valueToTree(row))) {
          List<String> assignments = new ArrayList<>();
          for (String c : t.columns.split(",")) assignments.add(c + "=?");
          jdbc.update(
              "UPDATE `" + t.name + "` SET " + String.join(",", assignments) + " WHERE " + where(t),
              values(row, t.columns + "," + t.keys));
        }
      }
    }
    for (int i = TABLES.length - 1; i >= 0; i--) {
      Table t = TABLES[i];
      Map<String, Object> remaining = index(rows(after, t), t);
      for (Object row : rows(before, t))
        if (!remaining.containsKey(key(row, t)))
          jdbc.update("DELETE FROM `" + t.name + "` WHERE " + where(t), values(row, t.keys));
    }
  }
}
