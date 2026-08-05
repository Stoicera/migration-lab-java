#!/usr/bin/env bash
# Verifies the reverse-proxy edge (ADR-0014) against a RUNNING stand.
#
# The edge is the only thing standing between a public demo and an anonymous
# POST /admin/bereinigen, so "we configured Traefik" is not a claim anyone should
# accept without a check. Everything this script asserts was first measured by
# hand on 2026-08-05 and then written down here, in that order.
#
#   MODERN_ADMIN_AUTH="admin:$(openssl passwd -apr1 'pw')" \
#     docker compose -f modern/docker-compose.yml -f modern/docker-compose.edge.yml up -d --wait
#   EDGE_USER=admin EDGE_PASSWORD=pw modern/edge/verify-edge.sh
#
# Exit 0 = the boundary holds · 1 = at least one assertion failed (all are reported).

set -uo pipefail

EDGE="${EDGE_URL:-http://localhost:8091}"
APP="${APP_URL:-http://localhost:8090}"
USER_NAME="${EDGE_USER:?EDGE_USER is not set}"
PASSWORD="${EDGE_PASSWORD:?EDGE_PASSWORD is not set}"

fails=0
pass() { printf '  ok    %s\n' "$1"; }
fail() {
	printf '  FAIL  %s\n' "$1" >&2
	fails=$((fails + 1))
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

# Wait for the edge to ROUTE, not merely to run. Traefik's Docker provider discovers
# containers asynchronously, so for a second or two after start the process answers
# and every path 404s. Measured on 2026-08-05: run straight after `up -d --wait` this
# script reported 17 failures against a configuration that was completely correct —
# a false red, which costs exactly as much trust as a false green. The compose
# healthcheck now covers the same window; this stays because the script must also be
# correct when someone runs it by hand.
echo "0. Waiting for the edge to route (not just to be up)"
for attempt in $(seq 1 30); do
	if [ "$(status "$EDGE/")" = "200" ]; then
		echo "  ok    routing after ${attempt}x1s"
		break
	fi
	if [ "$attempt" -eq 30 ]; then
		echo "  FAIL  $EDGE/ never returned 200 within 30s — the edge is up but not routing." >&2
		echo "        Check that the app container carries the traefik.* labels:" >&2
		echo "        docker inspect modern-app-1 --format '{{json .Config.Labels}}'" >&2
		exit 1
	fi
	sleep 1
done

expect_status() {
	local want="$1" label="$2"
	shift 2
	local got
	got=$(status "$@")
	if [ "$got" = "$want" ]; then pass "$label ($got)"; else fail "$label: expected $want, got $got"; fi
}

echo "1. The destructive and administrative surface is closed without credentials"
expect_status 401 "GET  /admin                 rejected" "$EDGE/admin"
expect_status 401 "GET  /api/admin/statistik   rejected" "$EDGE/api/admin/statistik"
expect_status 401 "POST /admin/bereinigen      rejected" -X POST "$EDGE/admin/bereinigen"
# /actuator is closed too: health is ours to read, not a stranger's free recon
expect_status 401 "GET  /actuator/health       rejected" "$EDGE/actuator/health"

echo "2. …and open with them (a lock nobody can open is a broken lock, not a secure one)"
expect_status 200 "GET  /admin                 allowed" -u "$USER_NAME:$PASSWORD" "$EDGE/admin"
expect_status 200 "GET  /api/admin/statistik   allowed" -u "$USER_NAME:$PASSWORD" "$EDGE/api/admin/statistik"
expect_status 200 "GET  /actuator/health       allowed" -u "$USER_NAME:$PASSWORD" "$EDGE/actuator/health"

echo "3. Wrong credentials stay out"
expect_status 401 "GET  /admin  wrong password" -u "$USER_NAME:definitiv-falsch" "$EDGE/admin"

echo "4. The application itself stays public — the edge must not become an outage"
expect_status 200 "GET  /                      public" "$EDGE/"
expect_status 200 "GET  /api/kunden            public" "$EDGE/api/kunden"
expect_status 200 "GET  /rechnungen (SPA)      public" "$EDGE/rechnungen"

echo "5. Security headers are present on public responses"
headers=$(curl -sI "$EDGE/")
check_header() {
	local name="$1" want="$2"
	local line
	line=$(printf '%s' "$headers" | grep -i "^$name:" | tr -d '\r')
	if [ -z "$line" ]; then
		fail "header $name missing"
	elif printf '%s' "$line" | grep -qiF -- "$want"; then
		pass "header $name"
	else
		fail "header $name: expected to contain '$want', got '$line'"
	fi
}
check_header "X-Frame-Options" "DENY"
check_header "X-Content-Type-Options" "nosniff"
check_header "Referrer-Policy" "no-referrer"
# script-src strict is the load-bearing part of the policy — assert it verbatim so
# a future "just add unsafe-inline to make it work" cannot pass unnoticed
check_header "Content-Security-Policy" "script-src 'self'"
check_header "Content-Security-Policy" "frame-ancestors 'none'"
check_header "Content-Security-Policy" "object-src 'none'"

echo "6. The rate limiter fires (and the bare application does not have one)"
count_429() {
	local url="$1" n=0 i
	for i in $(seq 1 80); do
		[ "$(status "$url")" = "429" ] && n=$((n + 1))
	done
	echo "$n"
}
edge_429=$(count_429 "$EDGE/api/kunden")
if [ "$edge_429" -gt 0 ]; then
	pass "80 rapid requests through the edge -> $edge_429 x 429"
else
	fail "80 rapid requests through the edge produced no 429 — the limiter is not doing anything"
fi
app_429=$(count_429 "$APP/api/kunden")
if [ "$app_429" -eq 0 ]; then
	pass "the same burst straight at the app -> 0 x 429 (the limit lives in the edge, as designed)"
else
	fail "the app itself returned $app_429 x 429, which this setup does not explain"
fi

echo ""
if [ "$fails" -eq 0 ]; then
	echo "edge verification: all assertions hold"
	echo ""
	echo "Known blind spot, stated rather than implied: this script and the Selenium"
	echo "suite both check behaviour, and neither can see a Content-Security-Policy"
	echo "VIOLATION — on 2026-08-05, 32 of the suite's 34 scenarios ran green while the browser was"
	echo "blocking Angular's runtime styles. The header assertions above exist because"
	echo "of that; the visual check remains manual (docs/MANUAL_TASKS.md)."
	exit 0
fi
echo "edge verification: $fails assertion(s) failed" >&2
exit 1
