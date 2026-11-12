package com.example.minibank.account;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="bank_account")
public class BankAccount {
 @Id
 @JdbcTypeCode(SqlTypes.BINARY)
 @Column(name="id", columnDefinition="RAW(16)", length=16)
 private UUID id;
 @Column(name="account_number", length=40)
 private String accountNumber;
 @Column(name="customer_id")
 private Long customerId;
 @Column(name="currency", length=3)
 private String currency;
 @Column(name="balance", precision=19, scale=0)
 private BigDecimal balance;
 @Column(name="opening_balance", precision=19, scale=0)
 private BigDecimal openingBalance;
 @Column(name="status", length=16)
 private String status;
 public UUID getId() { return id; }
 public void setId(UUID value) { this.id=value; }
 public String getAccountNumber() { return accountNumber; }
 public void setAccountNumber(String value) { this.accountNumber=value; }
 public Long getCustomerId() { return customerId; }
 public void setCustomerId(Long value) { this.customerId=value; }
 public String getCurrency() { return currency; }
 public void setCurrency(String value) { this.currency=value; }
 public BigDecimal getBalance() { return balance; }
 public void setBalance(BigDecimal value) { this.balance=value; }
 public BigDecimal getOpeningBalance() { return openingBalance; }
 public void setOpeningBalance(BigDecimal value) { this.openingBalance=value; }
 public String getStatus() { return status; }
 public void setStatus(String value) { this.status=value; }
}
