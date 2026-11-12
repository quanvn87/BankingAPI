package com.example.minibank.customer;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.util.*;
public interface CustomerRepository extends JpaRepository<Customer,Long> {
 
}
