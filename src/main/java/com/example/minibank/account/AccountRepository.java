package com.example.minibank.account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface AccountRepository extends JpaRepository<BankAccount,UUID> {
 Optional<BankAccount> findByAccountNumber(String accountNumber);
 @Query(value="SELECT * FROM bank_account WHERE id=:id FOR UPDATE WAIT 5",nativeQuery=true)
 Optional<BankAccount> lockById(@Param("id") UUID id);
}
