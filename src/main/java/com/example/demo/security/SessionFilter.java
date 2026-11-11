package com.example.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 客户端不能通过请求头选择身份；只信任服务器创建的会话。 */
@Component
public class SessionFilter extends OncePerRequestFilter {
  private final ObjectMapper mapper;

  public SessionFilter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "same-origin");
    if (!request.getRequestURI().startsWith("/api/")) {
      chain.doFilter(request, response);
      return;
    }
    response.setHeader("Cache-Control", "no-store");
    HttpSession session = request.getSession(false);
    boolean write = !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod());
    if (write
        && (session == null
            || session.getAttribute("csrf") == null
            || !session.getAttribute("csrf").equals(request.getHeader("X-CSRF-Token")))) {
      fail(response, 403, "CSRF", "页面凭据失效，请刷新后重试");
      return;
    }
    String path = request.getRequestURI();
    if (path.startsWith("/api/auth/")) {
      chain.doFilter(request, response);
      return;
    }
    if (session == null || session.getAttribute("role") == null) {
      fail(response, 401, "UNAUTHORIZED", "请先登录");
      return;
    }
    boolean admin = "ADMIN".equals(session.getAttribute("role"));
    if (path.startsWith("/api/admin/") && !admin) {
      fail(response, 403, "FORBIDDEN", "需要管理员身份");
      return;
    }
    if (admin && !path.startsWith("/api/admin/") && !path.equals("/api/market")) {
      fail(response, 403, "FORBIDDEN", "请使用普通用户账号进行交易");
      return;
    }
    request.setAttribute("userId", admin ? 0 : session.getAttribute("principalId"));
    if (admin) request.setAttribute("adminId", session.getAttribute("principalId"));
    chain.doFilter(request, response);
  }

  private void fail(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    mapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
  }
}
