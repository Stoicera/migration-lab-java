# Run 2026-07-31 — the pre-registered generation

Executed under `PROTOCOL.md` **v1.0**, SHA-256 `e7d02d2adfe03ca0cfc690794fbcb7db834a967781eab2aec1f6884c97c41037`
(tag `ai-testgen-protocol-v1`). 24 calls: 6 units × 2 model arms × 2 corpora, k = 1,
temperature 0, `max_tokens 16000`. Total cost **€0.6482** at the ECB reference rate of
2026-07-31 (1 EUR = 1.1485 USD). No call was retried; no call was re-prompted.

## Layout

```
<model-slug>/<corpus>/<unit>/
  prompt.md                          the rendered prompt instance
  request.json  response.json        verbatim, exactly what went over the wire
  usage.json                         tokens, provider, latency, cost, protocol SHA-256
  as-generated/                      extracted class, or EXTRACTION-FAILED.txt
  measurements/as-generated/         surefire + JaCoCo + PIT reports, maven log
<model-slug>/<corpus>/measurements-as-generated.csv    one row per unit
```

`pipeline-selftest/` in the parent directory is **not** part of this run — it is the
synthetic self-test of the measurement script.

## Two things a reader should know before reading the numbers

**1. `finish_reason=length` is not "the model failed to write code".** Three cells exhausted
the pinned 16 000-token output budget. In two of them — arm M1 on the God service, in *both*
corpora — the whole budget went into reasoning tokens and the assistant message was never
started, so `content` was JSON `null` and the harness wrote the literal string `null` into
`EXTRACTION-FAILED.txt`. The complete response, reasoning included, is in `response.json`.
Per protocol these cells count as non-compiling and are not re-run; per amendment A1 later runs
record `finish_reason` explicitly. Read them as *"no answer within the pinned budget"*.

**2. Provider routing really does vary** (threat T3, now with evidence). Arm M1 was served by
Amazon Bedrock for all 12 calls. Arm M2 was served by five different backends across its 12
calls — Ionstream, Novita, Alibaba, Parasail, StreamLake — with no request-side difference.
Each call's serving provider is in its `usage.json`.

## Phase status

- **Phase A (as generated): complete** (2026-07-31). Every cell measured, failures included.
- **Phase B (time-boxed repair): complete** (2026-08-02). 11 cells repaired, 2 recorded
  `ABANDONED` (their output was the literal `null` — amendment A2.3), 11 needed no work.
  `measurements-repaired.csv` covers all 24 cells; `fix-log.csv` and `repair-effort.csv` in this
  directory are the aggregated logs.

## Two further things a reader should know

**3. One cell was decided by our extraction rule, not by the model** — arm M1, corpus A, S4
`Rechnung`. Its reply holds two ```` ```java ```` blocks: an abandoned draft with a malformed
import, the sentence *"Wait, I need to produce the correct final answer without mistakes"*, then a
complete 26-test class with the import correct. The pre-registered "first block" rule kept the
draft. **The cell was not re-extracted and the 12/24 compile rate stands as measured** — see
amendment A3. All 24 responses were scanned; this is the only affected cell.

**4. The repair-effort minutes are contaminated and are an upper bound.** The eleven cells were
repaired in parallel (amendment A2.1) and competed for one 8-core machine while each ran Maven
repeatedly; one cell recorded 24.7 wall-clock minutes for a build that worked for 5.6 seconds.
Use the **fix counts and categories** in `fix-log.csv`, which no scheduler can distort. Amendment
A4 has the full self-assessment.

## What happened to these tests afterwards

Six of the repaired corpus-B classes were **adopted into `modern/src/test`**
([ADR-0011](../../../docs/adr/0011-adopting-generated-tests.md)). **The artifacts in this
directory are immutable and were not touched by that** — the adopted files are copies with a
provenance header. If they diverge later, this directory still records exactly what the models
produced.
