package com.example.minibank.user;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface UserRepository extends JpaRepository<AppUser,Long> {
 Optional<AppUser> findByUsername(String username);
 @Query(value="SELECT * FROM app_user WHERE id=:id FOR UPDATE WAIT 5",nativeQuery=true)
 Optional<AppUser> lockById(@Param("id") Long id);
}
