#!/usr/bin/env bash
# Verify the DEPLOYED stands — the production sibling of modern/edge/verify-edge.sh.
#
# Same philosophy: verify rather than trust, from OUTSIDE the host. A green
# Dokploy tile proves an image was pulled and a container started, nothing more.
# Run from any machine with curl + openssl:
#
#   EDGE_USER=admin EDGE_PASSWORD=... LEGACY_USER=demo LEGACY_PASSWORD=... deploy/verify-live.sh
#
# Exits non-zero on the first failed assertion. Known blind spot, inherited from
# the local script and stated rather than implied: no automated check here can
# see a CSP VIOLATION — after any CSP or frontend change, open the site in a
# real browser and read the console (docs/MANUAL_TASKS.md §H).
set -euo pipefail

MODERN_URL="${MODERN_URL:-https://migration-lab.stoicera.com}"
LEGACY_URL="${LEGACY_URL:-https://migration-lab-legacy.stoicera.com}"
: "${EDGE_USER:?EDGE_USER is not set}"
: "${EDGE_PASSWORD:?EDGE_PASSWORD is not set}"
: "${LEGACY_USER:?LEGACY_USER is not set}"
: "${LEGACY_PASSWORD:?LEGACY_PASSWORD is not set}"

fail() { printf '  FAIL  %s\n' "$1"; exit 1; }
ok()   { printf '  ok    %s\n' "$1"; }

code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "1. Certificates are really issued (not self-signed fallbacks)"
for host in "${MODERN_URL#https://}" "${LEGACY_URL#https://}"; do
  issuer=$(echo | openssl s_client -connect "$host:443" -servername "$host" 2>/dev/null \
    | openssl x509 -noout -issuer)
  case "$issuer" in
    *"Let's Encrypt"*) ok "$host issuer: Let's Encrypt" ;;
    *) fail "$host issuer is '$issuer' — ACME did not complete" ;;
  esac
done

echo "2. HTTP redirects to HTTPS on both stands"
for url in "$MODERN_URL" "$LEGACY_URL"; do
  c=$(code -I "http://${url#https://}/")
  case "$c" in
    301|308) ok "http://${url#https://} -> $c" ;;
    *) fail "http://${url#https://} answered $c, expected a permanent redirect" ;;
  esac
done

echo "3. Modern stand: the boundary rejects without credentials…"
for path in /admin /api/admin/statistik /actuator/health; do
  [ "$(code "$MODERN_URL$path")" = "401" ] && ok "GET $path rejected (401)" \
    || fail "GET $path not 401 without credentials"
done
[ "$(code -X POST "$MODERN_URL/admin/bereinigen")" = "401" ] \
  && ok "POST /admin/bereinigen rejected (401)" \
  || fail "POST /admin/bereinigen not 401 — the destructive endpoint is open"

echo "4. …and opens with them (a lock nobody can open is a broken lock)"
for path in /admin /api/admin/statistik /actuator/health; do
  [ "$(code -u "$EDGE_USER:$EDGE_PASSWORD" "$MODERN_URL$path")" = "200" ] \
    && ok "GET $path allowed (200)" || fail "GET $path not 200 with credentials"
done

echo "5. Modern public surface stays public"
for path in / /api/kunden /rechnungen; do
  [ "$(code "$MODERN_URL$path")" = "200" ] && ok "GET $path public (200)" \
    || fail "GET $path not 200 — the gate became an outage"
done

echo "6. Security headers on public responses"
H=$(curl -s -D - -o /dev/null "$MODERN_URL/")
echo "$H" | grep -qi '^x-frame-options: DENY'          && ok "X-Frame-Options: DENY"        || fail "X-Frame-Options missing/wrong"
echo "$H" | grep -qi '^x-content-type-options: nosniff' && ok "X-Content-Type-Options"      || fail "X-Content-Type-Options missing"
echo "$H" | grep -qi '^referrer-policy: no-referrer'    && ok "Referrer-Policy"             || fail "Referrer-Policy missing/wrong"
echo "$H" | grep -qi "^content-security-policy: .*script-src 'self'" && ok "CSP with strict script-src" \
  || fail "CSP missing or script-src relaxed"
if [ "${EXPECT_HSTS:-0}" = "1" ]; then
  echo "$H" | grep -qi '^strict-transport-security:' && ok "HSTS present" || fail "HSTS expected but missing"
fi

echo "7. Legacy stand: gated everywhere, open with the demo credential"
[ "$(code "$LEGACY_URL/")" = "401" ] && ok "GET / rejected (401)" \
  || fail "legacy stand answers without credentials — SD-1 is public"
[ "$(code "$LEGACY_URL/api/kunden")" = "401" ] && ok "GET /api/kunden rejected (401)" \
  || fail "legacy API answers without credentials"
[ "$(code -u "$LEGACY_USER:$LEGACY_PASSWORD" "$LEGACY_URL/")" = "200" ] \
  && ok "GET / with credentials (200)" || fail "legacy credential does not open"
[ "$(code -u "$LEGACY_USER:$LEGACY_PASSWORD" "$LEGACY_URL/api/kunden")" = "200" ] \
  && ok "GET /api/kunden with credentials (200)" || fail "legacy API not 200 with credentials"

echo "8. The rate limiter fires through the public edge"
# one concurrent burst, machine-independent (the CI lesson from 2026-08-05:
# a shell loop of sequential curls is a timing assertion, not a burst)
n429=$(seq 1 200 | xargs -P 0 -I{} -n1 curl -s -o /dev/null -w '%{http_code}\n' "$MODERN_URL/api/kunden" 2>/dev/null | grep -c '^429' || true)
[ "$n429" -gt 0 ] && ok "200 concurrent requests -> $n429 x 429" \
  || fail "no 429 out of 200 concurrent requests — limiter not engaging"

echo
echo "live verification: all assertions hold"
