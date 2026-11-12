package com.example.minibank.transfer;
import jakarta.validation.constraints.*;
import java.util.UUID;
import java.time.Instant;
import com.example.minibank.common.Money;
public final class TransferDtos {
 private TransferDtos() {}
 public record TransferCommand(@NotNull UUID sourceAccountId,@NotNull UUID destinationAccountId,
  @NotNull String amount,@Size(max=140) String description) {}
 public record TransferResponse(Long id,UUID sourceAccountId,UUID destinationAccountId,String amount,String currency,String description,Instant createdAt) {
  public static TransferResponse from(BankTransfer t) { return new TransferResponse(t.getId(),t.getSourceAccountId(),t.getDestinationAccountId(),Money.format(t.getAmount()),t.getCurrency(),t.getDescription()==null?"":t.getDescription(),t.getCreatedAt()); }
 }
 public record TransferOutcome(TransferResponse transfer,boolean replay) {}
}

