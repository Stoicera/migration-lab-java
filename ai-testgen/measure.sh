#!/usr/bin/env bash
# Step 4/6 of PROTOCOL.md §6: take the generated (or repaired) test class of one run,
# place it in the corpus's testbed, and measure it — compile, run, JaCoCo, PIT — writing
# every report back next to the run artifacts.
#
#   ./ai-testgen/measure.sh <date> <model-slug> <A|B> [as-generated|repaired]
#
# Example:
#   ./ai-testgen/measure.sh 2026-08-01 anthropic_claude-sonnet-5 A as-generated
#
# Rules this script implements, straight from the protocol:
#   - a failing or non-compiling test is a MEASUREMENT, not a broken build; the run
#     continues and the outcome is recorded (compile=FAILED / tests failed)
#   - PIT is scoped to the class under test only, DEFAULTS mutators
#   - nothing here edits generated code
set -u -o pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

date_arg="${1:?usage: measure.sh <date> <model-slug> <A|B> [phase]}"
model_slug="${2:?missing model slug, e.g. anthropic_claude-sonnet-5}"
corpus="${3:?missing corpus: A or B}"
phase="${4:-as-generated}"

case "$corpus" in
  A) module=legacy ;;
  B) module=modern ;;
  *) echo "corpus must be A or B" >&2; exit 2 ;;
esac

run_dir="ai-testgen/runs/$date_arg/$model_slug/$corpus"
testbed="ai-testgen/testbed/$module"
gen_dir="$testbed/src/test/java/at/werkstatt/crm/gen"

[ -d "$run_dir" ] || { echo "no such run: $run_dir" >&2; exit 2; }

summary="$run_dir/measurements-$phase.csv"
echo "unit,classUnderTest,compile,testsRun,failures,errors,lineCovered,lineMissed,branchCovered,branchMissed,mutationsGenerated,mutationsKilled" > "$summary"

for unit_dir in "$run_dir"/*/; do
  unit="$(basename "$unit_dir")"
  [ -d "$unit_dir/$phase" ] || { echo "skip $unit: no $phase/ directory"; continue; }

  class_under_test="$(python3 -c "
import json,sys
print(json.load(open('$unit_dir/usage.json'))['classUnderTest'])" 2>/dev/null)"
  if [ -z "${class_under_test:-}" ]; then
    echo "skip $unit: usage.json has no classUnderTest" >&2
    continue
  fi

  echo "=== measuring $corpus/$unit ($class_under_test, $phase) ==="
  # one class at a time: PIT is scoped per unit, so the testbed holds exactly one test class
  rm -f "$gen_dir"/*.java
  if ! compgen -G "$unit_dir/$phase/*.java" > /dev/null; then
    # extraction failure or an abandoned repair: recorded as non-compiling, measured as 0
    echo "$unit,$class_under_test,NO_CODE,0,0,0,0,0,0,0,0,0" >> "$summary"
    continue
  fi
  cp "$unit_dir/$phase"/*.java "$gen_dir/"

  # a re-measurement must not read the previous measurement's reports either
  out_dir="$unit_dir/measurements/$phase"
  rm -rf "$out_dir"
  mkdir -p "$out_dir"

  compile_status=OK
  # One invocation for everything, and `clean` is not optional:
  #   - leftover test-classes and reports from an earlier unit (or from the
  #     harness-validation profile) would be re-run and re-counted — the self-test that
  #     found this reported a compile-failed unit with the PREVIOUS unit's numbers;
  #   - PIT in a SEPARATE invocation sees no compiled sources ("no tests or no production
  #     code") because the added source root is registered during generate-sources.
  # A compile failure aborts before PIT, which is correct: there is nothing to mutate.
  ./mvnw -B -f "$testbed/pom.xml" -DtestFailureIgnore=true \
    "-Dpit.targetClasses=$class_under_test" \
    clean test org.pitest:pitest-maven:mutationCoverage \
    > "$out_dir/maven-test.log" 2>&1 || compile_status=FAILED
  grep -q "COMPILATION ERROR" "$out_dir/maven-test.log" && compile_status=COMPILE_FAILED

  cp -r "$testbed/target/surefire-reports" "$out_dir/" 2>/dev/null || true
  cp "$testbed/target/site/jacoco/jacoco.xml" "$out_dir/" 2>/dev/null || true
  cp -r "$testbed/target/pit-reports" "$out_dir/" 2>/dev/null || true

  python3 - "$unit" "$class_under_test" "$compile_status" "$out_dir" >> "$summary" <<'PY'
import glob, os, sys, xml.etree.ElementTree as ET

unit, cut, compile_status, out_dir = sys.argv[1:5]
tests = failures = errors = 0
for report in glob.glob(os.path.join(out_dir, "surefire-reports", "*.xml")):
    root = ET.parse(report).getroot()
    if root.tag == "testsuite":
        tests += int(root.get("tests", 0))
        failures += int(root.get("failures", 0))
        errors += int(root.get("errors", 0))

# coverage counts for the class under test only (PROTOCOL.md §7)
line_c = line_m = branch_c = branch_m = 0
jacoco = os.path.join(out_dir, "jacoco.xml")
if os.path.exists(jacoco):
    target = cut.replace(".", "/")
    for cls in ET.parse(jacoco).getroot().iter("class"):
        if cls.get("name") == target:
            for counter in cls.findall("counter"):
                if counter.get("type") == "LINE":
                    line_c, line_m = int(counter.get("covered")), int(counter.get("missed"))
                elif counter.get("type") == "BRANCH":
                    branch_c, branch_m = int(counter.get("covered")), int(counter.get("missed"))

generated = killed = 0
pit = os.path.join(out_dir, "pit-reports", "mutations.xml")
if os.path.exists(pit):
    for mutation in ET.parse(pit).getroot().iter("mutation"):
        generated += 1
        if mutation.get("status") == "KILLED":
            killed += 1

print(f"{unit},{cut},{compile_status},{tests},{failures},{errors},"
      f"{line_c},{line_m},{branch_c},{branch_m},{generated},{killed}")
PY
done

rm -f "$gen_dir"/*.java
echo
echo "summary written: $summary"
column -s, -t < "$summary"
