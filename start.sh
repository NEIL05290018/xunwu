#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then echo 'Copy .env.example to .env and configure your local MySQL first.' >&2; exit 1; fi
while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line%$'\r'}"
  [[ -z "$line" || "$line" == \#* ]] && continue
  key="${line%%=*}"
  if [[ "$key" =~ ^[A-Z][A-Z0-9_]*$ && "$line" == *=* ]]; then export "$key=${line#*=}"; fi
done < .env
java_command=java
[[ -n "${JAVA_HOME:-}" ]] && java_command="$JAVA_HOME/bin/java"
exec "$java_command" -jar release/xunwu-demo-4.1.0.jar
