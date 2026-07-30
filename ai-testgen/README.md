# ai-testgen/ — measured AI-assisted test generation

Experiment harness for LLM-generated unit tests against untestable legacy classes,
run in **G6**. The empirical protocol is **pre-registered**: `PROTOCOL.md` is written
and committed *before* any generation runs, and results are never curated afterwards.
Failed generations stay in the repo.

Method (see `docs/SPEC.md` §5):

1. Select N legacy classes (stratified: God service, mappers, controllers).
2. Generate unit tests via an `LlmClient` abstraction (OpenRouter; 2 models compared;
   prompts versioned here).
3. Evaluate: compile rate → pass-against-legacy rate → JaCoCo coverage →
   **PIT mutation score** → human-fix effort in minutes, logged.
4. Publish `REPORT.md` (German summary + English detail) including token/EUR costs.

Status: **empty — work starts in G6, protocol first.**
