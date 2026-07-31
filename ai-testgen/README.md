# ai-testgen/ — measured AI-assisted test generation

Experiment harness for LLM-generated unit tests against untestable legacy classes and
their migrated counterparts, run in **G6**. The empirical protocol is **pre-registered**:
[`PROTOCOL.md`](PROTOCOL.md) was written, reviewed and **frozen** (tag
`ai-testgen-protocol-v1`, 2026-07-31) *before* any generation ran, and results are never
curated afterwards. Failed generations stay in the repo.

**Status: infrastructure complete and validated, generation pending an API key.**
Everything that does not need a credential is done and green: protocol frozen, both
testbeds building, harness tested, dry-run through the full measurement pipeline.

## Layout

| Path | What it is |
|---|---|
| `PROTOCOL.md` | The frozen pre-registration. Authoritative; changes only as dated amendments. |
| `harness/` | Renders prompts, calls OpenRouter once per unit (`LlmClient` seam), records request/response/usage verbatim, extracts the code mechanically. Has its own tests, incl. the drift test that pins the protocol's prompt templates to the code that sends them. |
| `testbed/legacy/`, `testbed/modern/` | Measurement environments. Each compiles its corpus's `src/main/java` as an extra source root (corpus A at `--release 8`) and hosts the generated tests. `legacy/pom.xml` and `modern/pom.xml` are never touched, and **no test ever lands in `legacy/`**. |
| `harness-validation/` | Hand-written smoke tests, one per stratum shape per corpus. They validate the *pipeline* (§5 freeze gate) and are excluded from every experiment result. |
| `runs/` | Per-run artifacts: prompt instance, request, response, usage, `as-generated/` (broken files included), `repaired/`, JaCoCo + PIT reports, fix log. Created by the generation step. |
| `REPORT.md` | Results: German summary + English detail, incl. costs. Written after the run. |

## Running it

```bash
# what would run, and what it has cost so far — no key needed
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java -Dexec.args="plan"

# prompt instances only, no API call (--out inspects without creating run artifacts)
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="render --corpus A --model anthropic/claude-sonnet-5 --out /tmp/prompts"

# the generation step — needs OPENROUTER_API_KEY (see .env.example)
export OPENROUTER_API_KEY='sk-or-...'
./mvnw -q -f ai-testgen/harness/pom.xml compile exec:java \
  -Dexec.args="generate --corpus A --model anthropic/claude-sonnet-5"

# measurement pipeline (this is what the dry-run proved works, on both corpora)
./mvnw -Pvalidation -f ai-testgen/testbed/legacy/pom.xml test org.pitest:pitest-maven:mutationCoverage
```

The harness refuses any model outside the frozen price table, aborts at the €20 budget cap
computed from all recorded usage, and never edits generated code or re-prompts.

## Rules that make the numbers worth reading

- Non-compiling output is measured as 0, never excluded.
- Extraction failures count as non-compiling and are not re-prompted.
- Denominators always include failures.
- N = 6 per corpus, k = 1 — **no statistical significance is claimed anywhere**.
- Phase-B repair is done by the executing AI agent under supervision, not by a human with
  a stopwatch, and is labelled that way everywhere (PROTOCOL.md §6, threat T8, ADR-0010).
