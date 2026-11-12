package com.example.minibank.user;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="app_user")
public class AppUser {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="user_seq_gen")
 @SequenceGenerator(name="user_seq_gen",sequenceName="user_seq",allocationSize=1)
 private Long id;
 @Column(name="username", length=64)
 private String username;
 @Column(name="password_hash", length=100)
 private String passwordHash;
 @Column(name="role", length=16)
 private String role;
 @Convert(converter=org.hibernate.type.NumericBooleanConverter.class)
 @JdbcTypeCode(SqlTypes.NUMERIC)
 @Column(name="enabled", columnDefinition="NUMBER(1,0)")
 private boolean enabled;
 @Column(name="customer_id")
 private Long customerId;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public String getUsername() { return username; }
 public void setUsername(String value) { this.username=value; }
 public String getPasswordHash() { return passwordHash; }
 public void setPasswordHash(String value) { this.passwordHash=value; }
 public String getRole() { return role; }
 public void setRole(String value) { this.role=value; }
 public boolean getEnabled() { return enabled; }
 public void setEnabled(boolean value) { this.enabled=value; }
 public Long getCustomerId() { return customerId; }
 public void setCustomerId(Long value) { this.customerId=value; }
}
