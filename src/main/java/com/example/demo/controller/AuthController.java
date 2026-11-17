package com.example.demo.controller;

import static com.example.demo.service.BusinessException.Kind.*;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final com.example.demo.service.AuthService service;

  public AuthController(com.example.demo.service.AuthService service) {
    this.service = service;
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
    var principal = service.login(input.account(), input.password(), input.role());
    if (principal == null)
      return ResponseEntity.status(401).body(Map.of("code", "UNAUTHORIZED", "message", "账号或密码错误"));
    authenticate(request, principal.id(), principal.nickname(), principal.role());
    return ResponseEntity.ok(session(request));
  }

  @PostMapping("/register")
  public ResponseEntity<SessionInfo> register(
      @RequestBody Registration input, HttpServletRequest request) {
    var principal = service.register(input.nickname(), input.phone(), input.password());
    authenticate(request, principal.id(), principal.nickname(), principal.role());
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
