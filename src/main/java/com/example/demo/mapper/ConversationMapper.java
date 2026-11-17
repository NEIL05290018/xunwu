package com.example.demo.mapper;

import com.example.demo.model.Conversation;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** conversations 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface ConversationMapper {
  List<Conversation> findAll();

  int insert(Conversation value);

  Conversation findById(@Param("id") int id);

  int update(Conversation value);

  int deleteById(@Param("id") int id);

  Conversation findPair(@Param("first") int first, @Param("second") int second);
}
