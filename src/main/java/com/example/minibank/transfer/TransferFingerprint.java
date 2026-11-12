package com.example.minibank.transfer;
import com.example.minibank.common.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
public final class TransferFingerprint {
 private TransferFingerprint() {}
 public static String of(TransferDtos.TransferCommand command) {
  try {
   byte[] canonical=new ObjectMapper().writeValueAsBytes(List.of(command.sourceAccountId().toString(),
      command.destinationAccountId().toString(),Money.format(Money.parsePositive(command.amount())),
      command.description()==null ? "" : command.description()));
   return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical));
  } catch(com.fasterxml.jackson.core.JsonProcessingException | java.security.NoSuchAlgorithmException e) {
   throw new IllegalStateException("Cannot compute request fingerprint",e);
  }
 }
}
