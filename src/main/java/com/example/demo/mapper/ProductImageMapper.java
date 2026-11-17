package com.example.demo.mapper;

import com.example.demo.model.ProductImage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** product_images 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface ProductImageMapper {
  List<ProductImage> findAll();

  int insert(ProductImage value);

  ProductImage findById(@Param("id") int id);

  int update(ProductImage value);

  int deleteById(@Param("id") int id);

  int deleteByProduct(@Param("id") int id);
}
