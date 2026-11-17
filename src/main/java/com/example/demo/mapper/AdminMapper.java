package com.example.demo.mapper;

import com.example.demo.model.Admin;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** admins 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface AdminMapper {
  List<Admin> findAll();

  int insert(Admin value);

  Admin findById(@Param("id") int id);

  int update(Admin value);

  int deleteById(@Param("id") int id);

  Admin findByLoginName(@Param("login") String login);
}
