package com.example.demo.mapper;

import com.example.demo.dto.*;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MarketQueryMapper {
  JoinedProduct detail(@Param("uid") int uid, @Param("id") int id);

  List<JoinedProduct> favorites(@Param("uid") int uid);

  List<PurchaseSummary> purchases(@Param("uid") int uid);
}
