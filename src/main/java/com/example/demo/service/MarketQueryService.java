package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.mapper.MarketQueryMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarketQueryService {
  private final MarketQueryMapper mapper;

  public MarketQueryService(MarketQueryMapper mapper) {
    this.mapper = mapper;
  }

  public JoinedProduct detail(int uid, int id) {
    JoinedProduct p = mapper.detail(uid, id);
    if (p == null) throw new BusinessException(BusinessException.Kind.NOT_FOUND, "商品不存在或尚未公开");
    return p;
  }

  public List<JoinedProduct> favorites(int uid) {
    return mapper.favorites(uid);
  }

  public List<PurchaseSummary> purchases(int uid) {
    return mapper.purchases(uid);
  }
}
