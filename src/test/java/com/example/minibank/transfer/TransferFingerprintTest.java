package com.example.minibank.transfer;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class TransferFingerprintTest {
 UUID a=UUID.fromString("10000000-0000-4000-8000-000000000001"),b=UUID.fromString("f0000000-0000-4000-8000-000000000002");
 @Test void fingerprintSeparatesIntentAndNormalizesEmptyDescription() {
  var x=new TransferDtos.TransferCommand(a,b,"100",null);
  assertThat(TransferFingerprint.of(x)).matches("[0-9a-f]{64}");
  assertThat(TransferFingerprint.of(x)).isEqualTo(TransferFingerprint.of(new TransferDtos.TransferCommand(a,b,"100","")));
  assertThat(TransferFingerprint.of(x)).isNotEqualTo(TransferFingerprint.of(new TransferDtos.TransferCommand(a,b,"101","")));
  assertThat(TransferFingerprint.of(x)).isNotEqualTo(TransferFingerprint.of(new TransferDtos.TransferCommand(b,a,"100","")));
  assertThat(TransferFingerprint.of(x)).isNotEqualTo(TransferFingerprint.of(new TransferDtos.TransferCommand(a,b,"100"," ")));
 }
}
