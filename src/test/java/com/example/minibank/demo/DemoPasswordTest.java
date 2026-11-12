package com.example.minibank.demo;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class DemoPasswordTest {
 @Test void rejectsMissingShortAndBcryptTruncatedPasswords() {
  assertThatThrownBy(()->DemoSeeder.validatePassword(null)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->DemoSeeder.validatePassword("short")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->DemoSeeder.validatePassword("a".repeat(73))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->DemoSeeder.validatePassword("é".repeat(37))).isInstanceOf(IllegalArgumentException.class);
  assertThatCode(()->DemoSeeder.validatePassword("LearningPass_123")).doesNotThrowAnyException();
 }
}
