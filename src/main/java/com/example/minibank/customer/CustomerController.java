package com.example.minibank.customer;
import jakarta.validation.Valid;
import com.example.minibank.security.BankPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import static com.example.minibank.customer.CustomerDtos.*;
@RestController @RequestMapping("/api/v1/customers")
public class CustomerController {
 private final CustomerService service;
 public CustomerController(CustomerService service) { this.service=service; }
 @PostMapping public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request,@AuthenticationPrincipal BankPrincipal actor) {
  var result=service.create(request,actor);return ResponseEntity.created(URI.create("/api/v1/customers/"+result.id())).body(result);
 }
 @GetMapping("/{id}") public CustomerResponse get(@PathVariable Long id,@AuthenticationPrincipal BankPrincipal actor) { return service.get(id,actor); }
}
