package com.example.minibank.common;
import java.math.BigDecimal;
public final class Money {
 public static final BigDecimal MAX=new BigDecimal("9999999999999999999");
 private Money() {}
 public static BigDecimal parsePositive(String value) { if (value == null || !value.matches("[1-9][0-9]{0,18}"))
   throw new ApiException(400,"INVALID_AMOUNT","Số tiền phải là chuỗi số nguyên dương, tối đa 19 chữ số.");
  BigDecimal amount=new BigDecimal(value);
  if(amount.compareTo(MAX)>0) throw new ApiException(400,"INVALID_AMOUNT","Số tiền vượt giới hạn.");
  return amount; }
 public static String format(BigDecimal value) { return value.toPlainString(); }
}
