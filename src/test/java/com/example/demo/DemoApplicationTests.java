package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class DemoApplicationTests extends DatabaseTestSupport {

  @Test
  void contextLoads() {}
}
