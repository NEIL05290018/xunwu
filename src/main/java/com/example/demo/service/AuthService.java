package com.example.demo.service;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.mapper.*;
import com.example.demo.model.*;
import com.example.demo.security.Passwords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserMapper users;
  private final AdminMapper admins;

  public AuthService(UserMapper users, AdminMapper admins) {
    this.users = users;
    this.admins = admins;
  }

  public record Principal(int id, String nickname, String role) {}

  @Transactional(readOnly = true)
  public Principal login(String account, String password, String role) {
    if (account == null || account.length() > 80 || password == null || password.length() > 72)
      throw new BusinessException(INVALID, "请填写账号和密码");
    if ("ADMIN".equals(role)) {
      Admin a = admins.findByLoginName(account.trim());
      if (a == null || !Passwords.matches(password, a.password_hash)) return null;
      return new Principal(a.admin_id, a.login_name, "ADMIN");
    }
    User u = users.findByPhone(account.trim());
    if (u == null || !Passwords.matches(password, u.password_hash)) return null;
    if (!"正常".equals(u.account_status)) throw new BusinessException(FORBIDDEN, "账号已停用");
    return new Principal(u.user_id, u.nickname, "USER");
  }

  @Transactional
  public Principal register(String nickname, String phone, String password) {
    if (nickname == null
        || nickname.isBlank()
        || nickname.trim().length() > 40
        || phone == null
        || !phone.matches("1[0-9]{10}"))
      throw new BusinessException(INVALID, "昵称需 1—40 个字符，手机号需为 11 位数字");
    Passwords.validate(password);
    if (users.findByPhone(phone) != null) throw new BusinessException(CONFLICT, "该手机号已经注册");
    User u = new User();
    u.nickname = nickname.trim();
    u.phone = phone;
    u.account_status = "正常";
    u.password_hash = Passwords.hash(password);
    users.insert(u);
    return new Principal(u.user_id, u.nickname, "USER");
  }
}
