package com.example.minibank.demo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.LoggerFactory;
@Component @Profile("demo")
public class DemoBootstrap implements ApplicationRunner {
 private final DemoSeeder seeder;private final String an,binh,staff;
 public DemoBootstrap(DemoSeeder seeder,@Value("${demo.an-password:}") String an,@Value("${demo.binh-password:}") String binh,@Value("${demo.staff-password:}") String staff) {
  this.seeder=seeder;this.an=an;this.binh=binh;this.staff=staff;
 }
 @Override public void run(ApplicationArguments args) {
  var result=seeder.seed(an,binh,staff);
  LoggerFactory.getLogger(DemoBootstrap.class).info("Demo account UUIDs: An={}, Binh={}",result.anAccountId(),result.binhAccountId());
 }
}
