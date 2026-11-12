package com.example.minibank.transfer;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import com.example.minibank.security.BankPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import static com.example.minibank.transfer.TransferDtos.*;
@RestController @RequestMapping("/api/v1/transfers")
public class TransferController {
 private final TransferService service;
 public TransferController(TransferService service) { this.service=service; }
 @PostMapping public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal BankPrincipal actor,
   @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody TransferCommand request) {
  var result=service.transfer(actor,key,request);
  return ResponseEntity.status(result.replay()?200:201).location(URI.create("/api/v1/transfers/"+result.transfer().id())).body(result.transfer());
 }
 @GetMapping("/{id}") public TransferResponse get(@PathVariable Long id,@AuthenticationPrincipal BankPrincipal actor) { return service.get(id,actor); }
}
