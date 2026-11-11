package com.example.demo.repository;

import com.example.demo.model.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** 商品六方法。工作副本重载让商品、多图和收藏在同一数据库事务内提交。 */
@Repository
public class ProductRepository {
  private final MarketRepository market;

  public ProductRepository(MarketRepository market) {
    this.market = market;
  }

  public List<Product> findAll() {
    return findAll(market.snapshot());
  }

  public List<Product> findAll(MarketState state) {
    return new ArrayList<>(state.products);
  }

  public Product findById(int id) {
    return findById(market.snapshot(), id);
  }

  public Product findById(MarketState state, int id) {
    for (Product p : state.products) if (p.product_id == id) return p;
    return null;
  }

  public List<Product> findByCategory(String category) {
    return findByCategory(market.snapshot(), category);
  }

  public List<Product> findByCategory(MarketState state, String category) {
    List<Product> result = new ArrayList<>();
    for (Product p : state.products) if (Objects.equals(category, p.category)) result.add(p);
    return result;
  }

  public Product save(Product product) {
    return market.transaction(
        new MarketRepository.Operation<Product>() {
          @Override
          public Product apply(MarketState state) {
            return save(state, product);
          }
        });
  }

  public Product save(MarketState state, Product product) {
    product.product_id = market.allocateProductId();
    state.products.add(product);
    return product;
  }

  public Product update(int id, Product product) {
    return market.transaction(
        new MarketRepository.Operation<Product>() {
          @Override
          public Product apply(MarketState state) {
            return update(state, id, product);
          }
        });
  }

  public Product update(MarketState state, int id, Product product) {
    for (int i = 0; i < state.products.size(); i++)
      if (state.products.get(i).product_id == id) {
        product.product_id = id;
        state.products.set(i, product);
        return product;
      }
    return null;
  }

  public boolean deleteById(int id) {
    return market.transaction(
        new MarketRepository.Operation<Boolean>() {
          @Override
          public Boolean apply(MarketState state) {
            return deleteById(state, id);
          }
        });
  }

  public boolean deleteById(MarketState state, int id) {
    for (int i = 0; i < state.products.size(); i++)
      if (state.products.get(i).product_id == id) {
        state.products.remove(i);
        return true;
      }
    return false;
  }

  public boolean hasHistory(MarketState state, int id) {
    for (TradeOrder o : state.orders) if (o.product_id == id) return true;
    for (Report r : state.reports) if (r.product_id == id) return true;
    return false;
  }

  public void replaceImages(MarketState state, int productId, List<String> images) {
    int next = 0;
    for (ProductImage image : state.product_images) next = Math.max(next, image.image_id);
    for (int i = state.product_images.size() - 1; i >= 0; i--)
      if (state.product_images.get(i).product_id == productId) state.product_images.remove(i);
    for (String url : images) {
      ProductImage image = new ProductImage();
      image.image_id = ++next;
      image.product_id = productId;
      image.image_url = url;
      state.product_images.add(image);
    }
  }
}
