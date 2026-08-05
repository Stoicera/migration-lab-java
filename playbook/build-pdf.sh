#!/usr/bin/env bash
# Builds the playbook as one PDF — the artefact MILESTONES G7 asks CI to produce.
#
# Runs through the pandoc/latex container rather than a locally installed toolchain,
# for one reason: CI must run the SAME command as a laptop. GitHub's ubuntu-latest
# ships pandoc but no TeX engine, so a `pandoc -o x.pdf` step would work here and fail
# there — the kind of divergence this repository keeps arguing against.
#
#   playbook/build-pdf.sh                 -> playbook/dist/migration-playbook.pdf
#   OUT=/tmp/x.pdf playbook/build-pdf.sh
#
# Needs only Docker. Exit 0 = built and non-empty.

set -euo pipefail

cd "$(dirname "$0")/.."

OUT="${OUT:-playbook/dist/migration-playbook.pdf}"
IMAGE="${PANDOC_IMAGE:-pandoc/latex:3.7}"
mkdir -p "$(dirname "$OUT")"

# README first (it is the table of contents in prose), then the chapters in order.
mapfile -t CHAPTERS < <(ls playbook/[0-9][0-9]-*.md | sort)
if [ "${#CHAPTERS[@]}" -eq 0 ]; then
	echo "build-pdf: no chapters found — refusing to build an empty playbook" >&2
	exit 1
fi
echo "building ${#CHAPTERS[@]} chapters -> $OUT"

# --fail-if-warnings is deliberately NOT used: pandoc warns about the link-free
# chapter style. Missing glyphs are caught explicitly below instead.
log=$(mktemp)
docker run --rm \
	-v "$PWD:/data" -w /data \
	"$IMAGE" \
	--pdf-engine=xelatex \
	--include-in-header=playbook/pdf/header.tex \
	--metadata-file=playbook/pdf/metadata.yaml \
	--toc --toc-depth=2 \
	--number-sections \
	-V lang=de \
	-V geometry:margin=2.5cm \
	-V colorlinks=true \
	-o "$OUT" \
	playbook/README.md "${CHAPTERS[@]}" 2>&1 | tee "$log"

if grep -q "There is no" "$log"; then
	echo "" >&2
	echo "build-pdf: the PDF contains missing glyphs — a character in the playbook has no" >&2
	echo "representation in the font. Add it to playbook/pdf/header.tex:" >&2
	grep -o "There is no . (U+[0-9A-F]*)" "$log" | sort -u >&2
	rm -f "$log"
	exit 1
fi
rm -f "$log"

if [ ! -s "$OUT" ]; then
	echo "build-pdf: $OUT is missing or empty" >&2
	exit 1
fi
echo "ok: $OUT ($(du -h "$OUT" | cut -f1))"
