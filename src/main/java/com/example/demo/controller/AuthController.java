package com.example.demo.controller;

import static com.example.demo.service.BusinessException.Kind.*;

import com.example.demo.model.User;
import com.example.demo.repository.MarketRepository;
import com.example.demo.security.Passwords;
import com.example.demo.service.BusinessException;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final JdbcTemplate jdbc;
  private final MarketRepository repository;

  public AuthController(JdbcTemplate jdbc, MarketRepository repository) {
    this.jdbc = jdbc;
    this.repository = repository;
  }

  public record Login(String account, String password, String role) {}

  public record Registration(String nickname, String phone, String password) {}

  public record SessionInfo(
      Integer user_id, Integer admin_id, String nickname, String role, String csrf) {}

  @GetMapping("/session")
  public SessionInfo session(HttpServletRequest request) {
    HttpSession s = request.getSession(true);
    if (s.getAttribute("csrf") == null) s.setAttribute("csrf", UUID.randomUUID().toString());
    Integer id = (Integer) s.getAttribute("principalId");
    String role = (String) s.getAttribute("role");
    return new SessionInfo(
        "USER".equals(role) ? id : null,
        "ADMIN".equals(role) ? id : null,
        (String) s.getAttribute("nickname"),
        role,
        (String) s.getAttribute("csrf"));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Login input, HttpServletRequest request) {
    if (input.account() == null
        || input.account().length() > 80
        || input.password() == null
        || input.password().length() > 72) throw new BusinessException(INVALID, "请填写账号和密码");
    boolean admin = "ADMIN".equals(input.role());
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            admin
                ? "SELECT admin_id AS id,login_name AS nickname,password_hash FROM admins WHERE"
                      + " login_name=?"
                : "SELECT user_id AS id,nickname,password_hash,account_status FROM users WHERE"
                      + " phone=?",
            input.account().trim());
    if (rows.isEmpty()
        || !Passwords.matches(input.password(), (String) rows.get(0).get("password_hash")))
      return ResponseEntity.status(401).body(Map.of("code", "UNAUTHORIZED", "message", "账号或密码错误"));
    Map<String, Object> row = rows.get(0);
    if (!admin && !"正常".equals(row.get("account_status")))
      throw new BusinessException(FORBIDDEN, "账号已停用");
    authenticate(
        request,
        ((Number) row.get("id")).intValue(),
        (String) row.get("nickname"),
        admin ? "ADMIN" : "USER");
    return ResponseEntity.ok(session(request));
  }

  @PostMapping("/register")
  public ResponseEntity<SessionInfo> register(
      @RequestBody Registration input, HttpServletRequest request) {
    if (input.nickname() == null
        || input.nickname().isBlank()
        || input.nickname().trim().length() > 40
        || input.phone() == null
        || !input.phone().matches("1[0-9]{10}"))
      throw new BusinessException(INVALID, "昵称需 1—40 个字符，手机号需为 11 位数字");
    Passwords.validate(input.password());
    String hash = Passwords.hash(input.password());
    // 注册使用同一事务写锁；账号唯一性同时由数据库唯一约束保障。
    int id =
        repository.transaction(
            new MarketRepository.Operation<Integer>() {
              @Override
              public Integer apply(com.example.demo.model.MarketState state) {
                for (User user : state.users)
                  if (user.phone.equals(input.phone()))
                    throw new BusinessException(CONFLICT, "该手机号已经注册");
                jdbc.update(
                    "INSERT INTO users(nickname,phone,account_status,password_hash)"
                        + " VALUES(?,?,'正常',?)",
                    input.nickname().trim(),
                    input.phone(),
                    hash);
                return jdbc.queryForObject(
                    "SELECT user_id FROM users WHERE phone=?", Integer.class, input.phone());
              }
            });
    authenticate(request, id, input.nickname().trim(), "USER");
    return ResponseEntity.status(201).body(session(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    HttpSession s = request.getSession(false);
    if (s != null) s.invalidate();
    return ResponseEntity.noContent().build();
  }

  private void authenticate(HttpServletRequest request, int id, String nickname, String role) {
    HttpSession previous = request.getSession(false);
    if (previous != null) previous.invalidate();
    HttpSession s = request.getSession(true);
    s.setAttribute("principalId", id);
    s.setAttribute("nickname", nickname);
    s.setAttribute("role", role);
    s.setAttribute("csrf", UUID.randomUUID().toString());
  }
}
