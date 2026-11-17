package com.example.demo.mapper;

import com.example.demo.model.Product;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** products 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface ProductMapper {
  List<Product> findAll();

  int insert(Product value);

  Product findById(@Param("id") int id);

  int update(Product value);

  int deleteById(@Param("id") int id);

  Product lockById(@Param("id") int id);

  List<Product> findByCategory(@Param("category") String category);

  List<Product> search(
      @Param("uid") int uid,
      @Param("scope") String scope,
      @Param("category") String category,
      @Param("keyword") String keyword);

  int historyCount(@Param("id") int id);
}
