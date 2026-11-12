package com.example.minibank.account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AccountOpeningRepository extends JpaRepository<AccountOpening,UUID> {
 
}
