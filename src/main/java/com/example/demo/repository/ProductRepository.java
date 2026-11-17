package com.example.demo.repository;

import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.Product;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 保留课程六方法；真正 SQL 交由 ProductMapper.xml 执行。 */
@Repository
public class ProductRepository {
  private final ProductMapper mapper;

  public ProductRepository(ProductMapper mapper) {
    this.mapper = mapper;
  }

  public List<Product> findAll() {
    return mapper.findAll();
  }

  public Product findById(int id) {
    return mapper.findById(id);
  }

  public List<Product> findByCategory(String category) {
    return mapper.findByCategory(category);
  }

  public Product save(Product value) {
    mapper.insert(value);
    return value;
  }

  public Product update(int id, Product value) {
    value.product_id = id;
    return mapper.update(value) == 0 ? null : value;
  }

  public boolean deleteById(int id) {
    return mapper.deleteById(id) > 0;
  }
}
