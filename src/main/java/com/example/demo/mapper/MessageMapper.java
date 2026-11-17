package com.example.demo.mapper;

import com.example.demo.model.Message;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** messages 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface MessageMapper {
  List<Message> findAll();

  int insert(Message value);

  Message findById(@Param("id") int id);

  int update(Message value);

  int deleteById(@Param("id") int id);
}
