# pipeline-selftest — NOT an experiment run

Synthetic inputs that prove the measurement step of `PROTOCOL.md` §6 works, including its
failure paths. Kept in the repository as evidence that the pipeline was validated against a
failing input *before* the protocol was frozen — and because it found two real defects in
`measure.sh` when it was written (stale `target/` contamination; PIT seeing no compiled code
when invoked separately).

Contents: one test class that compiles with one passing and one deliberately failing test,
and one file that is deliberately not valid Java.

**Excluded from every experiment result.** No model produced any of this, and no cost was
incurred (`costEur: 0`).
