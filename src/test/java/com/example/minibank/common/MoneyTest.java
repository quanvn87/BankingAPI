package com.example.minibank.common;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class MoneyTest {
 @Test void acceptsPositiveIntegerAndMaximum() {
  assertThat(Money.parsePositive("300000")).isEqualByComparingTo("300000");
  assertThat(Money.parsePositive("9999999999999999999")).isEqualByComparingTo("9999999999999999999");
  assertThat(Money.format(new BigDecimal("300000"))).isEqualTo("300000");
 }
 @ParameterizedTest @NullAndEmptySource
 @ValueSource(strings={"0","-1","1.0","1e3"," 1","1 ","+1","01","10000000000000000000"})
 void rejectsInvalidMoney(String input) {
  assertThatThrownBy(() -> Money.parsePositive(input)).isInstanceOf(ApiException.class);
 }
}
