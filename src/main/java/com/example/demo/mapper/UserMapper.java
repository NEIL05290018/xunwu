package com.example.demo.mapper;

import com.example.demo.model.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** users 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface UserMapper {
  List<User> findAll();

  int insert(User value);

  User findById(@Param("id") int id);

  int update(User value);

  int deleteById(@Param("id") int id);

  User findByPhone(@Param("phone") String phone);

  User lockById(@Param("id") int id);
}
