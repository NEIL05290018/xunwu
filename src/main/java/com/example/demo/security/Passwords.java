package com.example.demo.security;

import com.example.demo.service.BusinessException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 每个密码独立随机盐；只存 PBKDF2 哈希，不存明文。 */
public final class Passwords {
  private static final int ITERATIONS = 210000;

  public static void validate(String password) {
    if (password == null || password.length() < 8 || password.length() > 72)
      throw new BusinessException(BusinessException.Kind.INVALID, "密码需要 8—72 个字符；首次启动必须设置管理员密码");
  }

  public static String hash(String password) {
    validate(password);
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return ITERATIONS
        + ":"
        + Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  public static boolean matches(String password, String encoded) {
    if (password == null || password.length() > 72 || encoded == null || encoded.isBlank())
      return false;
    try {
      String[] parts = encoded.split(":");
      return MessageDigest.isEqual(
          Base64.getDecoder().decode(parts[2]),
          derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException("密码算法不可用", e);
    } finally {
      spec.clearPassword();
    }
  }

  private Passwords() {}
}
