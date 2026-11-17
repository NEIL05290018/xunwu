package com.example.demo.mapper;

import com.example.demo.model.Review;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** reviews 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface ReviewMapper {
  List<Review> findAll();

  int insert(Review value);

  Review findById(@Param("id") int id);

  int update(Review value);

  int deleteById(@Param("id") int id);

  int countByAuthor(@Param("order") int order, @Param("user") int user);
}
