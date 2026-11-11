package com.example.demo.model;

/** products：对应最终报告，数据访问只通过 Repository。 */
public class Product {
  public Integer product_id;
  public Integer seller_id;
  public String title;
  public String description;
  public Integer price_cents;
  public String category;
  public Integer support_express;
  public Integer support_pickup;
  public String product_status;
  public String audit_status;
  public Integer audit_admin_id;
  public String audit_note;

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Product value = new Product();

    public Builder product_id(Integer v) {
      value.product_id = v;
      return this;
    }

    public Builder seller_id(Integer v) {
      value.seller_id = v;
      return this;
    }

    public Builder title(String v) {
      value.title = v;
      return this;
    }

    public Builder description(String v) {
      value.description = v;
      return this;
    }

    public Builder price_cents(Integer v) {
      value.price_cents = v;
      return this;
    }

    public Builder category(String v) {
      value.category = v;
      return this;
    }

    public Builder support_express(Integer v) {
      value.support_express = v;
      return this;
    }

    public Builder support_pickup(Integer v) {
      value.support_pickup = v;
      return this;
    }

    public Builder product_status(String v) {
      value.product_status = v;
      return this;
    }

    public Builder audit_status(String v) {
      value.audit_status = v;
      return this;
    }

    public Builder audit_admin_id(Integer v) {
      value.audit_admin_id = v;
      return this;
    }

    public Builder audit_note(String v) {
      value.audit_note = v;
      return this;
    }

    public Product build() {
      Product p = new Product();
      p.product_id = value.product_id;
      p.seller_id = value.seller_id;
      p.title = value.title;
      p.description = value.description;
      p.price_cents = value.price_cents;
      p.category = value.category;
      p.support_express = value.support_express;
      p.support_pickup = value.support_pickup;
      p.product_status = value.product_status;
      p.audit_status = value.audit_status;
      p.audit_admin_id = value.audit_admin_id;
      p.audit_note = value.audit_note;
      return p;
    }
  }
}
