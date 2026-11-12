package com.example.minibank.audit;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name="audit_event")
public class AuditEvent {
 @Id
 @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="audit_seq_gen")
 @SequenceGenerator(name="audit_seq_gen",sequenceName="audit_seq",allocationSize=1)
 private Long id;
 @Column(name="actor_id")
 private Long actorId;
 @Column(name="action", length=40)
 private String action;
 @Column(name="resource_id", length=64)
 private String resourceId;
 @Column(name="request_id", length=64)
 private String requestId;
 @Column(name="created_at", columnDefinition="TIMESTAMP(6) WITH TIME ZONE")
 private Instant createdAt;
 public Long getId() { return id; }
 public void setId(Long value) { this.id=value; }
 public Long getActorId() { return actorId; }
 public void setActorId(Long value) { this.actorId=value; }
 public String getAction() { return action; }
 public void setAction(String value) { this.action=value; }
 public String getResourceId() { return resourceId; }
 public void setResourceId(String value) { this.resourceId=value; }
 public String getRequestId() { return requestId; }
 public void setRequestId(String value) { this.requestId=value; }
 public Instant getCreatedAt() { return createdAt; }
 public void setCreatedAt(Instant value) { this.createdAt=value; }
}
