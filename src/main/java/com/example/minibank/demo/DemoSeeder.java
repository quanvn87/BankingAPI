package com.example.minibank.demo;
import com.example.minibank.account.*;
import com.example.minibank.customer.*;
import com.example.minibank.user.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
@Service
public class DemoSeeder {
 private final UserRepository users;private final CustomerRepository customers;private final AccountRepository accounts;
 private final AccountOpeningRepository openings;private final PasswordEncoder encoder;
 public DemoSeeder(UserRepository users,CustomerRepository customers,AccountRepository accounts,AccountOpeningRepository openings,PasswordEncoder encoder) {
  this.users=users;this.customers=customers;this.accounts=accounts;this.openings=openings;this.encoder=encoder;
 }
 public record DemoInfo(UUID anAccountId,UUID binhAccountId) {}
 @Transactional(rollbackFor=Exception.class)
 public DemoInfo seed(String anPassword,String binhPassword,String staffPassword) {
  validatePassword(anPassword);validatePassword(binhPassword);validatePassword(staffPassword);
  var an=users.findByUsername("an");var binh=users.findByUsername("binh");var staff=users.findByUsername("staff");
  if(an.isPresent() || binh.isPresent() || staff.isPresent()) {
   if(an.isEmpty() || binh.isEmpty() || staff.isEmpty()) throw new IllegalStateException("Partial demo users exist; inspect database without deleting user data.");
   var a=accounts.findByAccountNumber("100000000001").orElseThrow(()->new IllegalStateException("Missing demo account An"));
   var b=accounts.findByAccountNumber("100000000002").orElseThrow(()->new IllegalStateException("Missing demo account Binh"));
   if(!a.getCustomerId().equals(an.get().getCustomerId()) || !b.getCustomerId().equals(binh.get().getCustomerId())) throw new IllegalStateException("Demo account ownership mismatch");
   return new DemoInfo(a.getId(),b.getId());
  }
  var customerAn=customer("DEMO_AN","An");var customerBinh=customer("DEMO_BINH","Bình");
  user("an",anPassword,"CUSTOMER",customerAn.getId());user("binh",binhPassword,"CUSTOMER",customerBinh.getId());user("staff",staffPassword,"STAFF",null);
  var a=account(customerAn.getId(),"100000000001","1000000");var b=account(customerBinh.getId(),"100000000002","200000");
  return new DemoInfo(a.getId(),b.getId());
 }
 public static void validatePassword(String password) {
  int length=password==null?0:password.getBytes(StandardCharsets.UTF_8).length;
  if(length<12 || length>72) throw new IllegalArgumentException("Demo passwords must have 12-72 UTF-8 bytes; set DEMO_*_PASSWORD locally.");
 }
 private Customer customer(String code,String name) {
  var c=new Customer();c.setCode(code);c.setName(name);c.setStatus("ACTIVE");return customers.save(c);
 }
 private void user(String name,String password,String role,Long customerId) {
  var u=new AppUser();u.setUsername(name);u.setPasswordHash(encoder.encode(password));u.setRole(role);u.setEnabled(true);u.setCustomerId(customerId);users.save(u);
 }
 private BankAccount account(Long customerId,String number,String initial) {
  var a=new BankAccount();a.setId(UUID.randomUUID());a.setCustomerId(customerId);a.setAccountNumber(number);a.setCurrency("VND");a.setStatus("ACTIVE");
  BigDecimal amount=new BigDecimal(initial);a.setBalance(amount);a.setOpeningBalance(amount);accounts.saveAndFlush(a);
  var o=new AccountOpening();o.setAccountId(a.getId());o.setAmount(amount);o.setCreatedAt(Instant.now());openings.save(o);return a;
 }
}
