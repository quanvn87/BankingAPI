package com.example.minibank.security;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
public record BankPrincipal(Long userId,Long customerId,String username,String password,String role,boolean enabled) implements UserDetails {
 @Override public String getUsername() { return username; }
 @Override public String getPassword() { return password; }
 @Override public boolean isEnabled() { return enabled; }
 @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
 @Override public String toString() { return "BankPrincipal[userId="+userId+"]"; }
 public boolean isStaff() { return "STAFF".equals(role); }
}
