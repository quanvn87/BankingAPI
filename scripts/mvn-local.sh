#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then
  for bank_jdk in "$PWD"/work/tooling/jdk-*; do
    if [[ -x "$bank_jdk/bin/java" ]]; then export JAVA_HOME="$bank_jdk"; break; fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
exec ./mvnw "$@"
