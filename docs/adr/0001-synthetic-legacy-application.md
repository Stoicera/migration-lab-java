# ADR-0001 — Synthetic legacy application (WerkstattCRM) instead of a real abandoned OSS project

Date: 2026-07-30 · Status: accepted · Deciders: Sebastian Kern (owner)

## Context

The PRD defines a kickoff gate: if a genuinely abandoned open-source
Java-8/Spring-Boot-1.x (+ AngularJS) web application with a permissive licence
(Apache-2.0/MIT) and a sensible size (~4–8k LOC backend) exists, migrate that —
higher credibility. Otherwise build the synthetic WerkstattCRM, transparently
labelled.

We searched GitHub on 2026-07-30 (repository search with licence/archived filters,
authenticated API inspection of poms, file trees, commit histories and licence
files) plus the known real-world candidates of the Spring-Boot-1.x/AngularJS era.

**Result: no candidate satisfies the gate.** The population of repos matching
*abandoned + permissive licence + Java 8/Boot 1.x + AngularJS + right size + real
legacy characteristics* is effectively empty. Closest near-misses, each verified
individually:

| Candidate | Licence | Size / stack | Last real activity | Disqualifier |
|---|---|---|---|---|
| `spring-petclinic/spring-petclinic-angularjs` | Apache-2.0 claimed in readme, **no LICENSE file** | 33 Java files (~2–3k LOC), Boot **2.1.3** + AngularJS 1.x | Apr 2020 | Canonical *clean sample*: already has tests (falsifies the "legacy has no tests" premise), Boot 2.1 start would delete the 1.5→2.7 playbook chapter, universally recognized as a demo |
| `GeorPavl/Warehouse-Management-FullStack-App` | Apache-2.0 (verified) | 53 Java files, Boot **2.4.5 + Java 11**, AngularJS/Metronic front end | Jul 2022 (12 commits, 1 author, 0 stars) | Wrong stack (no Java 8, no Boot 1.x jump); zero-star student project — provenance adds less credibility than a transparent synthetic app |
| `mraible/21-points` (v2.0 of 2017 was Boot 1.5 + AngularJS) | **No licence file** | JHipster-generated app | **Still maintained** (Jan 2026) | Not abandoned, not licensed; author already migrated it publicly himself |

Categorically ruled out: Tudu-Lists (GPLv3), Zafira/Zebrunner (became a 147 MB
product suite), Mifos community-app (MPL + far too large), OpenLMIS (AGPL),
`wyait/manage` (★443 but no licence), Camunda 7 webapps (embedded in a huge
platform); everything else found was tutorials, coursework, or unlicensed hobby
code.

## Decision

**Build the synthetic WerkstattCRM** (car-workshop CRM, SPEC §2) as the legacy
application, transparently labelled as *synthetic but pattern-faithful*.

Reasons, in order of weight:

1. **Licence/provenance safety** — the only option with zero legal risk (even the
   best real candidate lacks an actual LICENSE file).
2. **Stack fidelity** — only a synthetic app delivers exactly Java 8 + Spring Boot
   1.5 + AngularJS 1.8 + a JSP admin page, i.e. the full migration distance the
   playbook must cover. Every real candidate starts at Boot 2.x and would remove
   the hardest, most instructive chapter (1.5→2.7).
3. **Didactic completeness** — the catalogued smells (God class, field injection,
   SQL string concatenation, mixed UI tech, **no tests**) are the precondition for
   the safety-net story (G2) and the AI test-generation experiment (G6).
4. **Honesty preserved** — README and playbook state the synthetic origin
   prominently; every deliberate wart is catalogued in `legacy/LEGACY_NOTES.md`
   with the real-world smell it represents; this ADR documents the search and its
   negative result as evidence that the alternative was seriously pursued.

## Consequences

- G1 builds WerkstattCRM per SPEC §2; `legacy/LEGACY_NOTES.md` is a first-class
  deliverable and the completeness criterion for the module.
- README section "Honest limits" carries the synthetic-origin disclosure from G0
  onwards (already in place).
- The migration narrative gains full control over difficulty and scope; in
  exchange we accept the (documented) weakness that the app's *history* is not
  real — patterns are, history is not. The playbook states this scaling caveat.
- Should a suitable real abandoned application surface later, it does **not**
  replace WerkstattCRM mid-project; at most it becomes a follow-up case study.
