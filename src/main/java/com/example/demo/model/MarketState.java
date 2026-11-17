package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

/** 十二表查询结果的聚合容器；用于页面响应与首次种子，不能写回数据库，不包含额外关系表。 */
public class MarketState {
  public List<Admin> admins = new ArrayList<>();
  public List<User> users = new ArrayList<>();
  public List<Address> addresses = new ArrayList<>();
  public List<Product> products = new ArrayList<>();
  public List<ProductImage> product_images = new ArrayList<>();
  public List<TradeOrder> orders = new ArrayList<>();
  public List<Conversation> conversations = new ArrayList<>();
  public List<Message> messages = new ArrayList<>();
  public List<Refund> refunds = new ArrayList<>();
  public List<Review> reviews = new ArrayList<>();
  public List<Report> reports = new ArrayList<>();
  public List<Favorite> favorites = new ArrayList<>();
}
