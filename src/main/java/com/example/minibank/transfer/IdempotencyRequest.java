package com.example.minibank.transfer;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="idempotency_request")
public class IdempotencyRequest {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="idempotency_seq_gen")
 @SequenceGenerator(name="idempotency_seq_gen",sequenceName="idempotency_seq",allocationSize=1)
 private Long id;
 @Column(name="requester_id")
 private Long requesterId;
 @Column(name="idempotency_key", length=64)
 private String idempotencyKey;
 @Column(name="fingerprint", length=64)
 private String fingerprint;
 @Column(name="transfer_id")
 private Long transferId;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public Long getRequesterId() { return requesterId; }
 public void setRequesterId(Long value) { this.requesterId=value; }
 public String getIdempotencyKey() { return idempotencyKey; }
 public void setIdempotencyKey(String value) { this.idempotencyKey=value; }
 public String getFingerprint() { return fingerprint; }
 public void setFingerprint(String value) { this.fingerprint=value; }
 public Long getTransferId() { return transferId; }
 public void setTransferId(Long value) { this.transferId=value; }
}
