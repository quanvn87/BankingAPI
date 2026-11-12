package com.example.minibank.security;
import com.example.minibank.user.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class DatabaseUserDetailsService implements UserDetailsService {
 private final UserRepository users;
 public DatabaseUserDetailsService(UserRepository users) { this.users=users; }
 @Override @Transactional(readOnly=true)
 public UserDetails loadUserByUsername(String username) {
  var user=users.findByUsername(username).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
  return new BankPrincipal(user.getId(),user.getCustomerId(),user.getUsername(),user.getPasswordHash(),user.getRole(),user.getEnabled());
 }
}
