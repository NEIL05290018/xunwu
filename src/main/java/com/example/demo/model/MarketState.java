package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

/** 12 张数据库表的一次事务工作副本，不包含额外关系表。 */
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
