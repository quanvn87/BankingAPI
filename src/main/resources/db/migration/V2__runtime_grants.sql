-- The local setup creates BANK_APP before migration. For an external database,
-- create it first using the DBA; grants are intentionally specific and append-only.
GRANT SELECT, INSERT ON customer TO bank_app;
GRANT SELECT, INSERT ON app_user TO bank_app;
GRANT SELECT, INSERT, UPDATE ON bank_account TO bank_app;
GRANT SELECT, INSERT ON account_opening TO bank_app;
GRANT SELECT, INSERT ON bank_transfer TO bank_app;
GRANT SELECT, INSERT ON ledger_entry TO bank_app;
GRANT SELECT, INSERT ON idempotency_request TO bank_app;
GRANT SELECT, INSERT ON audit_event TO bank_app;
GRANT SELECT ON customer_seq TO bank_app;
GRANT SELECT ON user_seq TO bank_app;
GRANT SELECT ON account_number_seq TO bank_app;
GRANT SELECT ON transfer_seq TO bank_app;
GRANT SELECT ON ledger_seq TO bank_app;
GRANT SELECT ON idempotency_seq TO bank_app;
GRANT SELECT ON audit_seq TO bank_app;
