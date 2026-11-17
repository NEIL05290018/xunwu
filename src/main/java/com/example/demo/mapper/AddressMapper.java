package com.example.demo.mapper;

import com.example.demo.model.Address;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** addresses 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface AddressMapper {
  List<Address> findAll();

  int insert(Address value);

  Address findById(@Param("id") int id);

  int update(Address value);

  int deleteById(@Param("id") int id);
}
