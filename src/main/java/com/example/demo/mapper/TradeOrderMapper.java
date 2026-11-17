package com.example.demo.mapper;

import com.example.demo.model.TradeOrder;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** orders 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface TradeOrderMapper {
  List<TradeOrder> findAll();

  int insert(TradeOrder value);

  TradeOrder findById(@Param("id") int id);

  int update(TradeOrder value);

  int deleteById(@Param("id") int id);

  TradeOrder lockById(@Param("id") int id);

  int activeCount(@Param("id") int id);
}
