package com.example.demo.mapper;

import com.example.demo.model.Refund;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** refunds 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface RefundMapper {
  List<Refund> findAll();

  int insert(Refund value);

  Refund findById(@Param("id") int id);

  int update(Refund value);

  int deleteById(@Param("id") int id);

  Refund findByOrder(@Param("id") int id);

  Refund lockById(@Param("id") int id);
}
