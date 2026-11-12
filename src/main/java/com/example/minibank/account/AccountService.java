package com.example.minibank.account;
import com.example.minibank.common.ApiException;
import com.example.minibank.security.BankPrincipal;
import com.example.minibank.customer.CustomerRepository;
import com.example.minibank.audit.AuditService;
import com.example.minibank.transfer.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static com.example.minibank.account.AccountDtos.*;
@Service
public class AccountService {
 private final AccountRepository accounts; private final AccountOpeningRepository openings;
 private final CustomerRepository customers; private final LedgerRepository ledger; private final AuditService audit;
 private final EntityManager em;
 public AccountService(AccountRepository accounts,AccountOpeningRepository openings,CustomerRepository customers,LedgerRepository ledger,AuditService audit,EntityManager em) {
  this.accounts=accounts;this.openings=openings;this.customers=customers;this.ledger=ledger;this.audit=audit;this.em=em;
 }
 @Transactional(rollbackFor=Exception.class)
 public AccountResponse open(OpenAccountRequest request,BankPrincipal actor) {
  requireStaff(actor);var customer=customers.findById(request.customerId()).orElseThrow(ApiException::notFound);
  if(!"ACTIVE".equals(customer.getStatus())) throw new ApiException(422,"CUSTOMER_INACTIVE","Khách hàng không hoạt động.");
  var a=new BankAccount();a.setId(UUID.randomUUID());a.setCustomerId(customer.getId());
  a.setAccountNumber("10"+em.createNativeQuery("SELECT account_number_seq.NEXTVAL FROM dual").getSingleResult().toString());
  a.setCurrency("VND");a.setBalance(BigDecimal.ZERO);a.setOpeningBalance(BigDecimal.ZERO);a.setStatus("ACTIVE");accounts.saveAndFlush(a);
  var opening=new AccountOpening();opening.setAccountId(a.getId());opening.setAmount(BigDecimal.ZERO);opening.setCreatedAt(Instant.now());openings.save(opening);
  audit.record(actor.userId(),"OPEN_ACCOUNT",a.getId().toString());return AccountResponse.from(a);
 }
 @Transactional(readOnly=true)
 public AccountResponse get(UUID id,BankPrincipal actor) { return AccountResponse.from(visible(id,actor)); }
 @Transactional(rollbackFor=Exception.class)
 public AccountResponse changeStatus(UUID id,String status,BankPrincipal actor) {
  requireStaff(actor);if(!"ACTIVE".equals(status)&&!"BLOCKED".equals(status)) throw new ApiException(400,"INVALID_STATUS","Trạng thái không hợp lệ.");
  var a=accounts.lockById(id).orElseThrow(ApiException::notFound);a.setStatus(status);
  audit.record(actor.userId(),"SET_ACCOUNT_"+status,id.toString());return AccountResponse.from(a);
 }
 @Transactional(readOnly=true)
 public EntryPage entries(UUID id,int page,int size,BankPrincipal actor) {
  if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw new ApiException(400,"INVALID_PAGE","page >= 0, size trong khoảng 1–100, page*size không vượt 2147483647.");
  visible(id,actor);var result=ledger.findByAccountIdOrderByCreatedAtDescIdDesc(id,PageRequest.of(page,size));
  return new EntryPage(result.getContent().stream().map(EntryResponse::from).toList(),page,size,result.getTotalElements(),result.getTotalPages());
 }
 private BankAccount visible(UUID id,BankPrincipal actor) {
  var a=accounts.findById(id).orElseThrow(ApiException::notFound);
  if(!actor.isStaff()&&!a.getCustomerId().equals(actor.customerId())) throw ApiException.notFound();return a;
 }
 private void requireStaff(BankPrincipal actor) { if(!actor.isStaff()) throw new ApiException(403,"FORBIDDEN","Chỉ nhân viên được thao tác."); }
}
