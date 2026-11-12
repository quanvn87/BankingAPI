package com.example.minibank.audit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AuditRepository extends JpaRepository<AuditEvent,Long> {
 
}
