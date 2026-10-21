#!/usr/bin/env bash
# Refreshes the committed /nachweis evidence from CI. The deployed page shows
# modern/frontend/public/nachweis.json; deploy.yml cannot produce it itself, because the
# report needs both stands running and both suites executed (that is what modern-ci,
# legacy-ci and e2e do). So the evidence is produced by CI and committed through a PR.
#
#   scripts/nachweis/refresh.sh            # latest successful modern-ci run on master
#   scripts/nachweis/refresh.sh <run-id>   # a specific modern-ci run (e.g. of a PR)
#
# Then: review the diff (counts, verdicts), commit on a branch, open a PR. The file names
# the commit and the CI run it came from; that pair, not the deploy time, is what it proves.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
run="${1:-}"
if [ -z "$run" ]; then
  run=$(gh run list --workflow modern-ci.yml --branch master --status success --limit 1 \
    --json databaseId --jq '.[0].databaseId')
fi
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
gh run download "$run" -n nachweis -D "$tmp"
cp "$tmp/nachweis.json" modern/frontend/public/nachweis.json
node -e '
const d = require("./modern/frontend/public/nachweis.json"), c = d.charakterisierung;
console.log(`run ${process.argv[1]} · commit ${d.commit?.sha?.slice(0, 7)} · ${c.gesamt} Tests: ${c.identisch} identisch, ${c.sanktioniert} sanktioniert, ${c.regression} Regression · E2E ${d.e2e.legacy?.bestanden}/${d.e2e.legacy?.gesamt} + ${d.e2e.modern?.bestanden}/${d.e2e.modern?.gesamt} · Trivy ${d.imageScan?.gesamt}`);
' "$run"
