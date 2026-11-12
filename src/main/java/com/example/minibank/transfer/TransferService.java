package com.example.minibank.transfer;

import com.example.minibank.common.*;
import com.example.minibank.security.BankPrincipal;
import com.example.minibank.user.UserRepository;
import com.example.minibank.account.*;
import com.example.minibank.customer.CustomerRepository;
import com.example.minibank.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

import static com.example.minibank.transfer.TransferDtos.*;

@Service
public class TransferService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final TransferRepository transfers;
    private final LedgerRepository ledger;
    private final IdempotencyRepository requests;
    private final AuditService audit;

    public TransferService(
            UserRepository users,
            AccountRepository accounts,
            CustomerRepository customers,
            TransferRepository transfers,
            LedgerRepository ledger,
            IdempotencyRepository requests,
            AuditService audit
    ) {
        this.users = users;
        this.accounts = accounts;
        this.customers = customers;
        this.transfers = transfers;
        this.ledger = ledger;
        this.requests = requests;
        this.audit = audit;
    }

    @Transactional(rollbackFor = Exception.class)
    public TransferOutcome transfer(
            BankPrincipal actor,
            String key,
            TransferCommand command
    ) {
        if (!"CUSTOMER".equals(actor.role())) {
            throw new ApiException(
                    403,
                    "FORBIDDEN",
                    "Chỉ khách hàng được chuyển tiền."
            );
        }

        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ApiException(
                    400,
                    "INVALID_KEY",
                    "Idempotency-Key gồm 1–64 chữ, số, dấu _ hoặc -."
            );
        }

        if (command.sourceAccountId() == null
                || command.destinationAccountId() == null
                || command.sourceAccountId().equals(command.destinationAccountId())) {
            throw new ApiException(
                    400,
                    "INVALID_ACCOUNTS",
                    "Hai tài khoản phải khác nhau và không để trống."
            );
        }

        if (command.description() != null && command.description().length() > 140) {
            throw new ApiException(
                    400,
                    "INVALID_DESCRIPTION",
                    "Nội dung tối đa 140 ký tự."
            );
        }

        BigDecimal amount = Money.parsePositive(command.amount());
        String fingerprint = TransferFingerprint.of(command);

        var requester = users.lockById(actor.userId())
                .orElseThrow(ApiException::notFound);

        if (!requester.getEnabled() || !"CUSTOMER".equals(requester.getRole())) {
            throw new ApiException(
                    403,
                    "FORBIDDEN",
                    "Người dùng không được phép chuyển tiền."
            );
        }

        var existing = requests.findByRequesterIdAndIdempotencyKey(actor.userId(), key);

        if (existing.isPresent()) {
            if (!existing.get().getFingerprint().equals(fingerprint)) {
                throw new ApiException(
                        409,
                        "IDEMPOTENCY_CONFLICT",
                        "Khóa đã dùng với nội dung khác."
                );
            }

            var old = transfers.findById(existing.get().getTransferId())
                    .orElseThrow(() -> new IllegalStateException("Missing committed transfer"));

            return new TransferOutcome(TransferResponse.from(old), true);
        }

        // Canonical lexical UUID ordering; never signed UUID.compareTo or source-first ordering.
        List<UUID> ordered = new ArrayList<>(List.of(
                command.sourceAccountId(),
                command.destinationAccountId()
        ));
        ordered.sort(Comparator.comparing(UUID::toString));

        BankAccount first = accounts.lockById(ordered.get(0))
                .orElseThrow(ApiException::notFound);
        BankAccount second = accounts.lockById(ordered.get(1))
                .orElseThrow(ApiException::notFound);

        BankAccount source = first.getId().equals(command.sourceAccountId()) ? first : second;
        BankAccount destination = source == first ? second : first;

        if (!source.getCustomerId().equals(requester.getCustomerId())) {
            throw ApiException.notFound();
        }

        for (BankAccount a : List.of(source, destination)) {
            if (!"ACTIVE".equals(a.getStatus())) {
                throw new ApiException(
                        422,
                        "ACCOUNT_BLOCKED",
                        "Tài khoản không hoạt động."
                );
            }

            if (!"VND".equals(a.getCurrency())) {
                throw new ApiException(
                        422,
                        "CURRENCY_MISMATCH",
                        "Chỉ hỗ trợ VND."
                );
            }

            var customer = customers.findById(a.getCustomerId())
                    .orElseThrow(ApiException::notFound);

            if (!"ACTIVE".equals(customer.getStatus())) {
                throw new ApiException(
                        422,
                        "CUSTOMER_INACTIVE",
                        "Khách hàng không hoạt động."
                );
            }
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new ApiException(
                    422,
                    "INSUFFICIENT_FUNDS",
                    "Số dư không đủ."
            );
        }

        BigDecimal after = destination.getBalance().add(amount);

        if (after.compareTo(Money.MAX) > 0) {
            throw new ApiException(
                    422,
                    "BALANCE_LIMIT",
                    "Số dư người nhận vượt giới hạn."
            );
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(after);

        var t = new BankTransfer();
        t.setRequesterId(actor.userId());
        t.setSourceAccountId(source.getId());
        t.setDestinationAccountId(destination.getId());
        t.setAmount(amount);
        t.setCurrency("VND");
        t.setDescription(command.description() == null ? "" : command.description());
        t.setCreatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        transfers.save(t);

        entry(t, source, "DEBIT");
        entry(t, destination, "CREDIT");

        var request = new IdempotencyRequest();
        request.setRequesterId(actor.userId());
        request.setIdempotencyKey(key);
        request.setFingerprint(fingerprint);
        request.setTransferId(t.getId());
        requests.save(request);

        audit.record(actor.userId(), "TRANSFER", t.getId().toString());

        return new TransferOutcome(TransferResponse.from(t), false);
    }

    private void entry(
            BankTransfer transfer,
            BankAccount account,
            String direction
    ) {
        var e = new LedgerEntry();
        e.setTransferId(transfer.getId());
        e.setAccountId(account.getId());
        e.setDirection(direction);
        e.setAmount(transfer.getAmount());
        e.setBalanceAfter(account.getBalance());
        e.setCreatedAt(transfer.getCreatedAt());
        ledger.save(e);
    }

    @Transactional(readOnly = true)
    public TransferResponse get(Long id, BankPrincipal actor) {
        var t = transfers.findById(id)
                .orElseThrow(ApiException::notFound);

        if (!actor.isStaff()) {
            var source = accounts.findById(t.getSourceAccountId())
                    .orElseThrow(ApiException::notFound);
            var dest = accounts.findById(t.getDestinationAccountId())
                    .orElseThrow(ApiException::notFound);

            if (!source.getCustomerId().equals(actor.customerId())
                    && !dest.getCustomerId().equals(actor.customerId())) {
                throw ApiException.notFound();
            }
        }

        return TransferResponse.from(t);
    }
}
