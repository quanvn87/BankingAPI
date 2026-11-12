package com.example.minibank.audit;
import com.example.minibank.common.RequestIdFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.UUID;
@Service
public class AuditService {
 private final AuditRepository events;
 public AuditService(AuditRepository events) { this.events=events; }
 @Transactional(propagation=Propagation.MANDATORY)
 public void record(Long actorId,String action,String resourceId) {
  var event=new AuditEvent();event.setActorId(actorId);event.setAction(action);event.setResourceId(resourceId);
  String requestId=MDC.get(RequestIdFilter.ATTRIBUTE);
  event.setRequestId(requestId==null?UUID.randomUUID().toString():requestId);event.setCreatedAt(Instant.now());events.save(event);
 }
}
