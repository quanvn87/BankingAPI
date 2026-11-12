-- Account IDs are UUID v4 mapped to RAW(16); other IDs use sequences.
CREATE SEQUENCE customer_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE user_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE account_number_seq START WITH 1000000001 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE transfer_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE ledger_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE idempotency_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE audit_seq START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE TABLE customer (
 id NUMBER(19,0) PRIMARY KEY, code VARCHAR2(40 CHAR) NOT NULL UNIQUE,
 name VARCHAR2(100 CHAR) NOT NULL, status VARCHAR2(16 CHAR) NOT NULL,
 CONSTRAINT customer_status_ck CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE app_user (
 id NUMBER(19,0) PRIMARY KEY, username VARCHAR2(64 CHAR) NOT NULL UNIQUE,
 password_hash VARCHAR2(100 CHAR) NOT NULL, role VARCHAR2(16 CHAR) NOT NULL,
 enabled NUMBER(1,0) NOT NULL, customer_id NUMBER(19,0) UNIQUE REFERENCES customer(id),
 CONSTRAINT user_role_ck CHECK(role IN ('CUSTOMER','STAFF')),
 CONSTRAINT user_enabled_ck CHECK(enabled IN (0,1)),
 CONSTRAINT user_customer_ck CHECK((role='CUSTOMER' AND customer_id IS NOT NULL) OR (role='STAFF' AND customer_id IS NULL))
);
CREATE TABLE bank_account (
 id RAW(16) PRIMARY KEY, account_number VARCHAR2(40 CHAR) NOT NULL UNIQUE,
 customer_id NUMBER(19,0) NOT NULL REFERENCES customer(id), currency VARCHAR2(3 CHAR) NOT NULL,
 balance NUMBER(19,0) NOT NULL, opening_balance NUMBER(19,0) NOT NULL,
 status VARCHAR2(16 CHAR) NOT NULL,
 CONSTRAINT account_currency_ck CHECK(currency='VND'),
 CONSTRAINT account_balance_ck CHECK(balance>=0 AND opening_balance>=0),
 CONSTRAINT account_status_ck CHECK(status IN ('ACTIVE','BLOCKED'))
);
CREATE INDEX account_customer_ix ON bank_account(customer_id);
CREATE TABLE account_opening (
 account_id RAW(16) PRIMARY KEY REFERENCES bank_account(id), amount NUMBER(19,0) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 CONSTRAINT opening_amount_ck CHECK(amount>=0)
);
CREATE TABLE bank_transfer (
 id NUMBER(19,0) PRIMARY KEY, requester_id NUMBER(19,0) NOT NULL REFERENCES app_user(id),
 source_account_id RAW(16) NOT NULL REFERENCES bank_account(id),
 destination_account_id RAW(16) NOT NULL REFERENCES bank_account(id),
 amount NUMBER(19,0) NOT NULL, currency VARCHAR2(3 CHAR) NOT NULL,
 description VARCHAR2(140 CHAR), created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 CONSTRAINT transfer_amount_ck CHECK(amount>0), CONSTRAINT transfer_currency_ck CHECK(currency='VND'),
 CONSTRAINT transfer_accounts_ck CHECK(source_account_id<>destination_account_id)
);
CREATE INDEX transfer_requester_ix ON bank_transfer(requester_id);
CREATE INDEX transfer_source_ix ON bank_transfer(source_account_id);
CREATE INDEX transfer_destination_ix ON bank_transfer(destination_account_id);
CREATE TABLE ledger_entry (
 id NUMBER(19,0) PRIMARY KEY, transfer_id NUMBER(19,0) NOT NULL REFERENCES bank_transfer(id),
 account_id RAW(16) NOT NULL REFERENCES bank_account(id), direction VARCHAR2(8 CHAR) NOT NULL,
 amount NUMBER(19,0) NOT NULL, balance_after NUMBER(19,0) NOT NULL,
 created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
 CONSTRAINT ledger_direction_ck CHECK(direction IN ('DEBIT','CREDIT')),
 CONSTRAINT ledger_amount_ck CHECK(amount>0 AND balance_after>=0),
 CONSTRAINT ledger_pair_uk UNIQUE(transfer_id,direction)
);
CREATE INDEX ledger_history_ix ON ledger_entry(account_id,created_at,id);
CREATE TABLE idempotency_request (
 id NUMBER(19,0) PRIMARY KEY, requester_id NUMBER(19,0) NOT NULL REFERENCES app_user(id),
 idempotency_key VARCHAR2(64 CHAR) NOT NULL, fingerprint VARCHAR2(64 CHAR) NOT NULL,
 transfer_id NUMBER(19,0) NOT NULL UNIQUE REFERENCES bank_transfer(id),
 CONSTRAINT idempotency_request_uk UNIQUE(requester_id,idempotency_key)
);
CREATE TABLE audit_event (
 id NUMBER(19,0) PRIMARY KEY, actor_id NUMBER(19,0) NOT NULL REFERENCES app_user(id),
 action VARCHAR2(40 CHAR) NOT NULL, resource_id VARCHAR2(64 CHAR) NOT NULL,
 request_id VARCHAR2(64 CHAR) NOT NULL, created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX audit_actor_ix ON audit_event(actor_id,created_at);
