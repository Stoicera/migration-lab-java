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

- **Phase A (as generated): complete.** Every cell measured, failures included.
- **Phase B (time-boxed repair): not started.** It begins in the next session.
