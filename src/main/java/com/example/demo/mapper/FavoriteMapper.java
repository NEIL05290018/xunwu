package com.example.demo.mapper;

import com.example.demo.model.Favorite;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** favorites 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface FavoriteMapper {
  List<Favorite> findAll();

  int insert(Favorite value);

  int deleteByProduct(@Param("id") int id);

  int deletePair(@Param("user") int user, @Param("product") int product);
}
