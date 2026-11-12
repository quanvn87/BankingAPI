package com.example.minibank.transfer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface IdempotencyRepository extends JpaRepository<IdempotencyRequest,Long> {
 Optional<IdempotencyRequest> findByRequesterIdAndIdempotencyKey(Long requesterId,String idempotencyKey);
}
