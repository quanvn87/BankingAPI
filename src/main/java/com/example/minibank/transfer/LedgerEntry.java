package com.example.minibank.transfer;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="ledger_entry")
public class LedgerEntry {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="ledger_seq_gen")
 @SequenceGenerator(name="ledger_seq_gen",sequenceName="ledger_seq",allocationSize=1)
 private Long id;
 @Column(name="transfer_id")
 private Long transferId;
 @JdbcTypeCode(SqlTypes.BINARY)
 @Column(name="account_id", columnDefinition="RAW(16)", length=16)
 private UUID accountId;
 @Column(name="direction", length=8)
 private String direction;
 @Column(name="amount", precision=19, scale=0)
 private BigDecimal amount;
 @Column(name="balance_after", precision=19, scale=0)
 private BigDecimal balanceAfter;
 @Column(name="created_at", columnDefinition="TIMESTAMP(6) WITH TIME ZONE")
 private Instant createdAt;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public Long getTransferId() { return transferId; }
 public void setTransferId(Long value) { this.transferId=value; }
 public UUID getAccountId() { return accountId; }
 public void setAccountId(UUID value) { this.accountId=value; }
 public String getDirection() { return direction; }
 public void setDirection(String value) { this.direction=value; }
 public BigDecimal getAmount() { return amount; }
 public void setAmount(BigDecimal value) { this.amount=value; }
 public BigDecimal getBalanceAfter() { return balanceAfter; }
 public void setBalanceAfter(BigDecimal value) { this.balanceAfter=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
