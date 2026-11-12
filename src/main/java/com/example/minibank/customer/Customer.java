package com.example.minibank.customer;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="customer")
public class Customer {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="customer_seq_gen")
 @SequenceGenerator(name="customer_seq_gen",sequenceName="customer_seq",allocationSize=1)
 private Long id;
 @Column(name="code", length=40)
 private String code;
 @Column(name="name", length=100)
 private String name;
 @Column(name="status", length=16)
 private String status;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public String getCode() { return code; }
 public void setCode(String value) { this.code=value; }
 public String getName() { return name; }
 public void setName(String value) { this.name=value; }
 public String getStatus() { return status; }
 public void setStatus(String value) { this.status=value; }
}
