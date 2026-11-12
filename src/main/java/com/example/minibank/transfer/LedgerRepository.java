package com.example.minibank.transfer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface LedgerRepository extends JpaRepository<LedgerEntry,Long> {
 Page<LedgerEntry> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);
}
