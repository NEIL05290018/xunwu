package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.dto.MarketRequests.*;
import com.example.demo.dto.MarketResponses.*;
import com.example.demo.model.*;
import com.example.demo.repository.MarketRepository;
import com.example.demo.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogService extends MarketSupport {
  private final ProductRepository products;

  public CatalogService(
      MarketRepository repository, ProductRepository products, ObjectMapper mapper) {
    super(repository, mapper);
    this.products = products;
  }

  public List<ProductResponse> find(int uid, String category, String keyword, String scope) {
    require(List.of("market", "mine", "all").contains(scope), INVALID, "查询范围无效");
    MarketState s = repository.snapshot();
    user(s, uid);
    java.util.ArrayList<ProductResponse> result = new java.util.ArrayList<>();
    for (Product p :
        category == null || category.isBlank()
            ? products.findAll(s)
            : products.findByCategory(s, category))
      if (("mine".equals(scope)
              ? p.seller_id == uid
              : "all".equals(scope)
                  ? p.seller_id == uid || "通过".equals(p.audit_status)
                  : "通过".equals(p.audit_status) && "在售".equals(p.product_status))
          && (category == null || category.isBlank() || category.equals(p.category))
          && (keyword == null
              || keyword.isBlank()
              || (p.title + p.description).toLowerCase().contains(keyword.trim().toLowerCase())))
        result.add(dto(p, ProductResponse.class));
    return result;
  }

  public ProductResponse detail(int uid, int id) {
    MarketState s = repository.snapshot();
    user(s, uid);
    Product p = requiredProduct(s, id);
    require(p.seller_id == uid || "通过".equals(p.audit_status), FORBIDDEN, "商品尚未公开");
    return dto(p, ProductResponse.class);
  }

  public ProductResponse save(int uid, Integer id, ProductInput input) {
    return repository.transaction(
        new MarketRepository.Operation<ProductResponse>() {
          @Override
          public ProductResponse apply(MarketState s) {
            user(s, uid);
            String title = text(input.title(), 80), description = text(input.description(), 1000);
            require(
                input.price_cents() != null
                    && input.price_cents() > 0
                    && input.price_cents() <= 100000000,
                INVALID,
                "价格无效，金额单位为分");
            require(
                List.of("数码设备", "数码配件", "图书教材", "生活用品", "服饰箱包", "运动户外")
                    .contains(input.category() == null ? "" : input.category()),
                INVALID,
                "分类无效");
            require(
                Boolean.TRUE.equals(input.support_express())
                    || Boolean.TRUE.equals(input.support_pickup()),
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
            if (id == null) {
              p = Product.builder().seller_id(uid).build();
              products.save(s, p);
            } else {
              p = requiredProduct(s, id);
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
            products.update(s, p.product_id, p);
            products.replaceImages(s, p.product_id, images);
            return dto(p, ProductResponse.class);
          }
        });
  }

  private Product requiredProduct(MarketState state, int id) {
    Product p = products.findById(state, id);
    require(p != null, NOT_FOUND, "商品不存在");
    return p;
  }

  public void delete(int uid, int id) {
    repository.transaction(
        new MarketRepository.Operation<Void>() {
          @Override
          public Void apply(MarketState s) {
            user(s, uid);
            Product p = requiredProduct(s, id);
            require(p.seller_id == uid, FORBIDDEN, "只能删除自己的商品");
            require(
                !List.of("锁定", "已售").contains(p.product_status) && !products.hasHistory(s, id),
                CONFLICT,
                "有交易或举报历史的商品不可删除");
            for (int index = s.product_images.size() - 1; index >= 0; index--) {
              ProductImage i = s.product_images.get(index);
              if (i.product_id == id) s.product_images.remove(index);
            }
            for (int index = s.favorites.size() - 1; index >= 0; index--) {
              Favorite f = s.favorites.get(index);
              if (f.product_id == id) s.favorites.remove(index);
            }
            products.deleteById(s, id);
            return null;
          }
        });
  }

  public void favorite(int uid, int id, boolean add) {
    repository.transaction(
        new MarketRepository.Operation<Void>() {
          @Override
          public Void apply(MarketState s) {
            user(s, uid);
            Product p = requiredProduct(s, id);
            require(p.seller_id == uid || "通过".equals(p.audit_status), FORBIDDEN, "商品未公开");
            for (int index = s.favorites.size() - 1; index >= 0; index--) {
              Favorite f = s.favorites.get(index);
              if (f.user_id == uid && f.product_id == id) s.favorites.remove(index);
            }
            if (add) {
              Favorite f = new Favorite();
              f.user_id = uid;
              f.product_id = id;
              f.favorited_at = now();
              s.favorites.add(f);
            }
            return null;
          }
        });
  }

  public ProductResponse audit(int aid, int id, DecisionInput request) {
    return repository.transaction(
        new MarketRepository.Operation<ProductResponse>() {
          @Override
          public ProductResponse apply(MarketState s) {
            admin(s, aid);
            Product p = requiredProduct(s, id);
            require("待审核".equals(p.audit_status), CONFLICT, "商品已处理");
            require(request.agree() != null, INVALID, "请选择审核结果");
            p.audit_admin_id = aid;
            p.audit_note = text(request.note(), 500);
            p.audit_status = request.agree() ? "通过" : "驳回";
            p.product_status = request.agree() ? "在售" : "下架";
            return dto(p, ProductResponse.class);
          }
        });
  }
}
