package com.example.minibank.account;
import com.example.minibank.security.BankPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.UUID;
import java.net.URI;
import static com.example.minibank.account.AccountDtos.*;
@RestController @RequestMapping("/api/v1/accounts")
public class AccountController {
 private final AccountService service;
 public AccountController(AccountService service) { this.service=service; }
 @PostMapping public ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request,@AuthenticationPrincipal BankPrincipal actor) {
  var result=service.open(request,actor);return ResponseEntity.created(URI.create("/api/v1/accounts/"+result.id())).body(result);
 }
 @GetMapping("/{id}") public AccountResponse get(@PathVariable UUID id,@AuthenticationPrincipal BankPrincipal actor) { return service.get(id,actor); }
 @PatchMapping("/{id}/status") public AccountResponse status(@PathVariable UUID id,@Valid @RequestBody StatusRequest request,@AuthenticationPrincipal BankPrincipal actor) { return service.changeStatus(id,request.status(),actor); }
 @GetMapping("/{id}/entries") public EntryPage entries(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal BankPrincipal actor) { return service.entries(id,page,size,actor); }
}
