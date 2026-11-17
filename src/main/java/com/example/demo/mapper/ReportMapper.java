package com.example.demo.mapper;

import com.example.demo.model.Report;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** reports 表的数据访问接口；SQL 位于同名 Mapper XML。 */
public interface ReportMapper {
  List<Report> findAll();

  int insert(Report value);

  Report findById(@Param("id") int id);

  int update(Report value);

  int deleteById(@Param("id") int id);

  int upheldCount(@Param("id") int id);

  Report lockById(@Param("id") int id);
}
