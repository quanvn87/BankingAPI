package com.example.minibank.account;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="account_opening")
public class AccountOpening {
 @Id
 @JdbcTypeCode(SqlTypes.BINARY)
 @Column(name="account_id", columnDefinition="RAW(16)", length=16)
 private UUID accountId;
 @Column(name="amount", precision=19, scale=0)
 private BigDecimal amount;
 @Column(name="created_at", columnDefinition="TIMESTAMP(6) WITH TIME ZONE")
 private Instant createdAt;
 public UUID getAccountId() { return accountId; }
 public void setAccountId(UUID value) { this.accountId=value; }
 public BigDecimal getAmount() { return amount; }
 public void setAmount(BigDecimal value) { this.amount=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
