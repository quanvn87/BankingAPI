package com.example.minibank.customer;
import com.example.minibank.security.BankPrincipal;
import com.example.minibank.common.ApiException;
import com.example.minibank.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.minibank.customer.CustomerDtos.*;
@Service
public class CustomerService {
 private final CustomerRepository customers; private final AuditService audit;
 public CustomerService(CustomerRepository customers,AuditService audit) { this.customers=customers;this.audit=audit; }
 @Transactional(rollbackFor=Exception.class)
 public CustomerResponse create(CreateCustomerRequest request,BankPrincipal actor) {
  if(!actor.isStaff()) throw new ApiException(403,"FORBIDDEN","Chỉ nhân viên được tạo khách hàng.");
  var c=new Customer(); c.setName(request.name().strip());c.setStatus("ACTIVE");
  // Code is independent of sequential IDs and does not contain personal information.
  c.setCode("C"+java.util.UUID.randomUUID().toString());customers.save(c);
  audit.record(actor.userId(),"CREATE_CUSTOMER",c.getId().toString());return CustomerResponse.from(c);
 }
 @Transactional(readOnly=true)
 public CustomerResponse get(Long id,BankPrincipal actor) {
  if(!actor.isStaff() && !id.equals(actor.customerId())) throw ApiException.notFound();
  return CustomerResponse.from(customers.findById(id).orElseThrow(ApiException::notFound));
 }
}
