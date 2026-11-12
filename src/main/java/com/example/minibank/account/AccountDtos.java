package com.example.minibank.account;
import com.example.minibank.common.Money;
import com.example.minibank.transfer.LedgerEntry;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.Instant;
public final class AccountDtos {
 private AccountDtos() {}
 public record OpenAccountRequest(@NotNull @Positive Long customerId) {}
 public record StatusRequest(@NotNull @Pattern(regexp="ACTIVE|BLOCKED") String status) {}
 public record AccountResponse(UUID id,String accountNumber,Long customerId,String currency,String balance,String status) {
  static AccountResponse from(BankAccount a) { return new AccountResponse(a.getId(),a.getAccountNumber(),a.getCustomerId(),a.getCurrency(),Money.format(a.getBalance()),a.getStatus()); }
 }
 public record EntryResponse(Long id,Long transferId,String direction,String amount,String balanceAfter,Instant createdAt) {
  static EntryResponse from(LedgerEntry e) { return new EntryResponse(e.getId(),e.getTransferId(),e.getDirection(),Money.format(e.getAmount()),Money.format(e.getBalanceAfter()),e.getCreatedAt()); }
 }
 public record EntryPage(List<EntryResponse> content,int page,int size,long totalElements,int totalPages) {}
}
