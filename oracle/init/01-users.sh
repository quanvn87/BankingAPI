#!/usr/bin/env bash
set -euo pipefail
# SQL interpolation accepts only a documented restricted password alphabet.
for bank_password in "$BANK_OWNER_PASSWORD" "$BANK_APP_PASSWORD"; do
  if [[ ! "$bank_password" =~ ^[A-Za-z][A-Za-z0-9_]{11,63}$ ]]; then
    echo 'Database passwords must start with a letter and contain 12-64 letters/digits/underscores.' >&2
    exit 1
  fi
done
sqlplus -s / as sysdba <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE
ALTER SESSION SET CONTAINER=FREEPDB1;
CREATE USER bank_owner IDENTIFIED BY "$BANK_OWNER_PASSWORD" QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO bank_owner;
CREATE USER bank_app IDENTIFIED BY "$BANK_APP_PASSWORD";
GRANT CREATE SESSION TO bank_app;
EXIT;
SQL
