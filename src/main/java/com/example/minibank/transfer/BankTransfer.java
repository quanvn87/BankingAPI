package com.example.minibank.transfer;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="bank_transfer")
public class BankTransfer {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="transfer_seq_gen")
 @SequenceGenerator(name="transfer_seq_gen",sequenceName="transfer_seq",allocationSize=1)
 private Long id;
 @Column(name="requester_id")
 private Long requesterId;
 @JdbcTypeCode(SqlTypes.BINARY)
 @Column(name="source_account_id", columnDefinition="RAW(16)", length=16)
 private UUID sourceAccountId;
 @JdbcTypeCode(SqlTypes.BINARY)
 @Column(name="destination_account_id", columnDefinition="RAW(16)", length=16)
 private UUID destinationAccountId;
 @Column(name="amount", precision=19, scale=0)
 private BigDecimal amount;
 @Column(name="currency", length=3)
 private String currency;
 @Column(name="description", length=140)
 private String description;
 @Column(name="created_at", columnDefinition="TIMESTAMP(6) WITH TIME ZONE")
 private Instant createdAt;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public Long getRequesterId() { return requesterId; }
 public void setRequesterId(Long value) { this.requesterId=value; }
 public UUID getSourceAccountId() { return sourceAccountId; }
 public void setSourceAccountId(UUID value) { this.sourceAccountId=value; }
 public UUID getDestinationAccountId() { return destinationAccountId; }
 public void setDestinationAccountId(UUID value) { this.destinationAccountId=value; }
 public BigDecimal getAmount() { return amount; }
 public void setAmount(BigDecimal value) { this.amount=value; }
 public String getCurrency() { return currency; }
 public void setCurrency(String value) { this.currency=value; }
 public String getDescription() { return description; }
 public void setDescription(String value) { this.description=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
