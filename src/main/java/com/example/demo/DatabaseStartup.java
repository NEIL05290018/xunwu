package com.example.demo;

import com.example.demo.service.SeedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseStartup implements ApplicationRunner {
  private final SeedService seeds;

  public DatabaseStartup(SeedService seeds) {
    this.seeds = seeds;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    seeds.initialize();
  }
}
