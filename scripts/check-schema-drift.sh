#!/usr/bin/env bash
# Seed/schema drift guard — the characterization suite compares two stands, so the
# two stands must hold the same schema and the same data. If they drift, the
# equivalence gate keeps passing while comparing two different databases: a green
# suite that proves nothing. That failure mode is the reason this guard exists.
#
# Until stage 6 this was a plain `diff -q legacy/db/init/*.sql modern/db/init/*.sql`
# in legacy-ci.yml. Stage 6 moved the modern side to Flyway (ADR-0013), so the files
# now differ in their header comments and only in those — this script compares what
# the database actually executes: SQL statements, comments and blank lines removed.
#
# Usage: scripts/check-schema-drift.sh          (from the repository root)
# Exit:  0 = the two stands agree · 1 = drift, with a diff on stdout

set -euo pipefail

cd "$(dirname "$0")/.."

# Everything the server sees: strip whole-line comments, strip trailing comments,
# drop blank lines, collapse leading/trailing whitespace.
normalise() {
	sed -e 's/--.*$//' -e 's/[[:space:]]\+$//' -e 's/^[[:space:]]\+//' "$1" | grep -v '^$'
}

compare() {
	local label="$1" legacy_file="$2" modern_file="$3"
	for f in "$legacy_file" "$modern_file"; do
		if [ ! -f "$f" ]; then
			echo "drift guard: $f does not exist — the guard cannot pass by accident" >&2
			return 1
		fi
	done
	if diff -u <(normalise "$legacy_file") <(normalise "$modern_file") >/tmp/drift.$$ 2>&1; then
		echo "ok   $label: legacy and modern agree ($(normalise "$legacy_file" | wc -l) SQL lines)"
		rm -f /tmp/drift.$$
		return 0
	fi
	echo "DRIFT $label: $legacy_file vs $modern_file" >&2
	sed -e "s|^--- .*|--- $legacy_file (legacy stand)|" -e "s|^+++ .*|+++ $modern_file (modern stand)|" /tmp/drift.$$ >&2
	rm -f /tmp/drift.$$
	return 1
}

rc=0
compare "schema" legacy/db/init/01-schema.sql modern/src/main/resources/db/migration/V1__baseline_schema.sql || rc=1
compare "seed  " legacy/db/init/02-daten.sql modern/src/main/resources/db/demo/V2__demo_seed.sql || rc=1

if [ "$rc" -ne 0 ]; then
	echo "" >&2
	echo "The two stands no longer hold the same data. Either revert the change, or —" >&2
	echo "if the divergence is intended — register it in docs/adr/0004-functional-equivalence-and-sanctioned-divergence.md" >&2
	echo "BEFORE re-capturing any golden master (ADR-0007)." >&2
fi
exit "$rc"
