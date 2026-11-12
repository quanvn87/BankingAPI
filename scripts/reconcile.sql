-- Difference must be zero for every customer account.
SELECT RAWTOHEX(a.id) AS account_id, a.account_number, a.balance,
       a.opening_balance + COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount ELSE -e.amount END),0) AS expected_balance,
       a.balance - (a.opening_balance + COALESCE(SUM(CASE WHEN e.direction='CREDIT' THEN e.amount ELSE -e.amount END),0)) AS difference
FROM bank_account a LEFT JOIN ledger_entry e ON e.account_id=a.id
GROUP BY a.id,a.account_number,a.balance,a.opening_balance
ORDER BY a.account_number;
-- No rows expected: each transfer must have two balanced ledger entries.
SELECT transfer_id
FROM ledger_entry
GROUP BY transfer_id
HAVING COUNT(*)<>2 OR SUM(CASE WHEN direction='CREDIT' THEN amount ELSE -amount END)<>0;
