package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class CatalogService extends MarketSupport {
  private final ProductRepository products;

  public CatalogService(
      MarketRepository repository, ProductRepository products, ObjectMapper mapper) {
    super(repository, mapper);
    this.products = products;
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> find(int uid, String category, String keyword, String scope) {
    return find(uid, category, keyword, scope, null, null);
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> find(
      int uid, String category, String keyword, String scope, Integer minPrice, Integer maxPrice) {
    user(uid);
    require(
        (minPrice == null || minPrice >= 0 && minPrice <= 100000000)
            && (maxPrice == null || maxPrice >= 0 && maxPrice <= 100000000)
            && (minPrice == null || maxPrice == null || minPrice <= maxPrice),
        INVALID,
        "价格区间无效，单位为分");
    require(List.of("market", "mine", "all").contains(scope), INVALID, "查询范围无效");
    List<ProductResponse> result = new ArrayList<>();
    for (Product p :
        repository
            .product()
            .searchRange(
                uid,
                scope,
                category == null ? null : category.trim(),
                keyword == null ? null : keyword.trim(),
                minPrice,
                maxPrice)) result.add(dto(p, ProductResponse.class));
    return result;
  }

  @Transactional(readOnly = true)
  public ProductResponse detail(int uid, int id) {
    user(uid);
    Product p = product(id);
    require(p.seller_id == uid || "通过".equals(p.audit_status), FORBIDDEN, "商品尚未公开");
    return dto(p, ProductResponse.class);
  }

  public ProductResponse save(int uid, Integer id, ProductInput input) {
    user(uid);
    String title = text(input.title(), 80), description = text(input.description(), 1000);
    require(
        input.price_cents() != null && input.price_cents() > 0 && input.price_cents() <= 100000000,
        INVALID,
        "价格无效，金额单位为分");
    require(
        List.of("数码设备", "数码配件", "图书教材", "生活用品", "服饰箱包", "运动户外")
            .contains(input.category() == null ? "" : input.category()),
        INVALID,
        "分类无效");
    require(
        Boolean.TRUE.equals(input.support_express()) || Boolean.TRUE.equals(input.support_pickup()),
        INVALID,
        "至少选择一种交付方式");
    List<String> images =
        input.images() == null || input.images().isEmpty()
            ? List.of("/assets/bag.svg")
            : input.images();
    require(images.size() <= 4, INVALID, "最多上传四张图片");
    for (String image : images)
      require(
          image != null
              && image.length() <= 1400000
              && (image.matches("/assets/[a-z-]+\\.svg")
                  || image.matches("data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+")),
          INVALID,
          "图片格式或大小无效");
    Product p;
    if (id == null) p = Product.builder().seller_id(uid).build();
    else {
      p = lockProduct(id);
      require(p.seller_id == uid, FORBIDDEN, "只能编辑自己的商品");
      require(!List.of("锁定", "已售").contains(p.product_status), CONFLICT, "交易中的商品不能编辑");
    }
    p.title = title;
    p.description = description;
    p.price_cents = input.price_cents();
    p.category = input.category();
    p.support_express = Boolean.TRUE.equals(input.support_express()) ? 1 : 0;
    p.support_pickup = Boolean.TRUE.equals(input.support_pickup()) ? 1 : 0;
    p.product_status = "草稿";
    p.audit_status = "待审核";
    p.audit_admin_id = null;
    p.audit_note = null;
    if (id == null) products.save(p);
    else products.update(id, p);
    repository.productImage().deleteByProduct(p.product_id);
    for (String url : images) {
      ProductImage image = new ProductImage();
      image.product_id = p.product_id;
      image.image_url = url;
      repository.productImage().insert(image);
    }
    return dto(p, ProductResponse.class);
  }

  public void delete(int uid, int id) {
    user(uid);
    Product p = lockProduct(id);
    require(p.seller_id == uid, FORBIDDEN, "只能删除自己的商品");
    require(
        !List.of("锁定", "已售").contains(p.product_status)
            && repository.product().historyCount(id) == 0,
        CONFLICT,
        "有交易或举报历史的商品不可删除");
    repository.favorite().deleteByProduct(id);
    repository.productImage().deleteByProduct(id);
    products.deleteById(id);
  }

  public int deleteBatch(int uid, List<Integer> ids) {
    user(uid);
    require(ids != null && !ids.isEmpty() && ids.size() <= 100, INVALID, "请选择1至100件商品");
    for (Integer id : ids) require(id != null && id > 0, INVALID, "商品编号无效");
    List<Integer> sorted = new ArrayList<>(new TreeSet<>(ids));
    // 固定顺序锁定全部商品，校验全部通过后才删除；任意失败整批回滚。
    for (int id : sorted) {
      Product p = lockProduct(id);
      require(p.seller_id == uid, FORBIDDEN, "只能删除自己的商品");
      require(
          !List.of("锁定", "已售").contains(p.product_status)
              && repository.product().historyCount(id) == 0,
          CONFLICT,
          "有交易或举报历史的商品不可删除");
    }
    for (int id : sorted) {
      repository.favorite().deleteByProduct(id);
      repository.productImage().deleteByProduct(id);
    }
    return repository.product().deleteBatch(sorted);
  }

  public void favorite(int uid, int id, boolean add) {
    user(uid);
    Product p = lockProduct(id);
    require(p.seller_id == uid || "通过".equals(p.audit_status), FORBIDDEN, "商品未公开");
    repository.favorite().deletePair(uid, id);
    if (add) {
      Favorite f = new Favorite();
      f.user_id = uid;
      f.product_id = id;
      f.favorited_at = now();
      repository.favorite().insert(f);
    }
  }

  public ProductResponse audit(int aid, int id, DecisionInput input) {
    admin(aid);
    Product p = lockProduct(id);
    require("待审核".equals(p.audit_status), CONFLICT, "商品已处理");
    require(input.agree() != null, INVALID, "请选择审核结果");
    p.audit_admin_id = aid;
    p.audit_note = text(input.note(), 500);
    p.audit_status = input.agree() ? "通过" : "驳回";
    p.product_status = input.agree() ? "在售" : "下架";
    products.update(id, p);
    return dto(p, ProductResponse.class);
  }
}
