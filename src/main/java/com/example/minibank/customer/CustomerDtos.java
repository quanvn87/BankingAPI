package com.example.minibank.customer;
import jakarta.validation.constraints.*;
public final class CustomerDtos {
 private CustomerDtos() {}
 public record CreateCustomerRequest(@NotBlank @Size(max=100) String name) {}
 public record CustomerResponse(Long id,String code,String name,String status) {
  public static CustomerResponse from(Customer c) { return new CustomerResponse(c.getId(),c.getCode(),c.getName(),c.getStatus()); }
 }
}
