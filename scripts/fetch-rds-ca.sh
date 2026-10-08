#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p certs
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT
curl --fail --silent --show-error --proto '=https' --tlsv1.2 https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem -o "$TMP"
openssl crl2pkcs7 -nocrl -certfile "$TMP" | openssl pkcs7 -print_certs -noout >/dev/null
install -m 0644 "$TMP" certs/global-bundle.pem
sha256sum certs/global-bundle.pem
