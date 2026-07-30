# ADR-0002 — OpenRewrite as assistant, not autopilot

Date: 2026-07-30 · Status: accepted · Context: stage 4 (Boot 2.7 → 3.5 → 4.1)

## Context

Stage 4 was the first migration leg where automated recipes were available.
We ran `rewrite-maven-plugin` 6.45.0 with `rewrite-spring` 6.36.0 on `modern/`
and measured what the recipes actually delivered — the evaluation is a project
deliverable (SPEC §4), not a footnote.

**What the recipes did (measured):**

| Recipe | Result |
|---|---|
| `boot3.UpgradeSpringBoot_3_5` | Parent 2.7.18 → 3.5.16 ✔ · pinned `javax.servlet:jstl:1.2` in `dependencyManagement` ✘ |
| `boot4.UpgradeSpringBoot_4_0` | Parent → 4.0.7 ✔ · `spring-boot-starter-web` → `-webmvc` ✔ · JSP taglib URI → `jakarta.tags.core` ✔ · property key `spring.jackson.serialization.*` → `spring.jackson.datatype.datetime.*` ✔ (self-reported "20m saved") |
| `boot4.SpringBootProperties_4_1` | no changes (nothing to do in this app) |
| **No full `UpgradeSpringBoot_4_1` composite exists** | 4.1 parent bump done manually |

**What the recipes missed — and what that cost:**

1. **The JSTL trap (the expensive one).** Instead of migrating `javax.servlet:jstl`
   to the Jakarta artifacts, the Boot-3 recipe *pinned the 2009 javax version*.
   The build stayed green, the app started, the REST API worked — and the JSP
   admin page died at runtime with `ClassNotFoundException:
   javax.servlet.jsp.tagext.TagLibraryValidator` on Tomcat 10.1. **Compiles ≠ works.**
   Caught by the characterization suite (the admin-page capture), not by the compiler.
2. **Jackson 3 migration.** Boot 4 moved to Jackson 3 (`tools.jackson`);
   `Jackson2ObjectMapperBuilderCustomizer` → `JsonMapperBuilderCustomizer` with a
   different builder API. No recipe touched our customizer — this was the only
   compile break of the 3.5 → 4.1 leg and had to be found and fixed by hand.
3. **Nothing structural.** Field injection, God class, string-concatenated SQL:
   untouched, as expected — recipes migrate APIs, they do not improve designs.

## Decision

**Use OpenRewrite as an accelerator for mechanical, well-understood changes —
never as an unsupervised migration autopilot.** Concretely:

- Run recipes per leg, in a dedicated commit, and **review the diff line by line**
  before building. Recipe output is a proposal, not a result.
- Treat "build is green" as *no evidence at all* of a successful migration.
  The safety net decides — runtime, API contract and UI, per commit.
- Any recipe change that *pins* a legacy artifact instead of migrating it is a
  defect to be corrected by hand and documented.

## Consequences

- Stage 4 kept the recipes for the parent bumps, the starter rename, the taglib
  URI and the property key rewrite — a genuine time saver on mechanical edits.
- Three manual interventions were required (Jakarta JSTL artifacts, Jackson 3
  customizer, 4.1 parent bump). All are documented in playbook ch. 4 as the
  honest scorecard rather than a "fully automated migration" claim.
- The project can now answer the sales question — *"can AI/tooling just do our
  migration?"* — with measured evidence instead of opinion: it removes the
  typing, not the engineering.
