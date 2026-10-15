# Show HN — draft

> **Status: DRAFT (2026-10-15). Not posted.** The founder reads it, rewrites what does not sound
> like him, and posts it himself. Every number below carries its date; re-check them on the day
> of posting (see [checklist.md](checklist.md)).

## Rules this draft follows

Read on 2026-10-15 at <https://news.ycombinator.com/showhn.html> and
<https://news.ycombinator.com/newsguidelines.html>:

- Show HN is for something people can try, built by the poster, who can discuss it in the thread.
  Here: the repository (clone and `docker compose up`) and the public modern stand. Both work
  without a signup. The legacy stand is behind Basic auth and is **not** part of what we ask
  people to try; it runs locally from the repository instead.
- The title starts with "Show HN:", no marketing language, no superlative, max 80 characters.
- No asking friends or colleagues to upvote or comment, in any channel. No second account.
- Be ready to answer questions; accept criticism without arguing it away.

Our own posting rules on top: it is our project and we say so; the demo must answer on the day
(`curl` check in the checklist); the author answers comments for the whole day.

## Title (pick one)

1. `Show HN: A Spring Boot 1.5 to 4.1 migration in public, one test suite for both` (78 chars) — recommended
2. `Show HN: Migration-lab – a Java 8 app migrated to Java 25, same tests on both` (77 chars)

**URL:** https://github.com/Stoicera/migration-lab-java

## First comment by the author

> Hi HN, I'm Sebastian from a two-person software company in Upper Austria.
>
> Many companies here still run web applications from around 2016: Java 8, Spring Boot 1.x,
> AngularJS, an old PostgreSQL. The migration keeps getting postponed for the same reason: nobody
> knows completely what the old system does, so nobody would notice if the new one did it
> differently.
>
> migration-lab is our attempt to show, in public, how we de-risk that. It is a small CRM for a car
> repair shop, built the way it would have been built in 2016 (synthetic, but with catalogued
> legacy smells), and migrated in six tagged stages to Java 25 / Spring Boot 4.1 / Angular 22.
> Both stands run side by side; you can check out any stage tag and `docker compose up`.
>
> The part I would most like feedback on is the safety net, built before the first migration
> commit:
>
> - 47 characterization tests (golden masters over HTTP responses and database state) and 34
>   Selenium scenarios. CI runs the same suites against the 2016 stand and the 2026 stand; the
>   latest runs on master (2026-10-15) are green on both.
> - The Selenium page objects only know intent keys ("the search button"); one properties file
>   per UI maps them to selectors. Porting the suite to the new UI meant writing a second
>   properties file, not new tests.
> - Where the stands differ on purpose (a closed SQL injection, an absorbed JSP admin page, a
>   fixed `undefined` alert), the difference is registered in an ADR and pinned per stand by a
>   test.
>
> Things that did not go the way a sales deck would want, and are in the repo as such:
>
> - The modern stand is not measurably faster on our read-path load test. Two local runs, two
>   readings: 2026-08-05 p95 1.60 ms modern vs 1.56 ms legacy; 2026-10-02 p95 2.34 ms vs 3.99 ms,
>   but median 1.28 ms vs 1.17 ms. Ten years of framework versions bought supportability, not
>   speed, on this workload.
> - We measured LLM-generated unit tests under a protocol frozen before the first call. The first
>   result (100 % line coverage, 99.2 % mutation score) only covered the half of the test classes
>   that compiled. After a time-boxed repair, mutation score fell to 73.2 %. In our experiment
>   (six classes per codebase, one attempt each, July/August 2026) the generated tests found zero
>   real bugs.
> - With a strict CSP, the E2E suite ran 32 of 34 green while the browser was blocking Angular's
>   styles (2026-08-05). Selenium checks behaviour, not appearance.
>
> What it does not do: it is not a migration tool and not a framework; there is no "run this on
> your codebase" button. The app is small on purpose (about 1.7k lines of backend, 25 endpoints,
> 10 views), so the numbers do not scale linearly to a 500k-line system. It has no user identity
> (Basic auth at the edge is a lock, not an identity system). The playbook is in German.
>
> What is still rough: the Trivy scan of the modern image currently reports 3 CRITICAL findings in
> embedded Tomcat (2026-10-15; the scan reports, it does not fail the build), and 14 Dependabot
> PRs are open.
>
> How the work was done, and why the logged effort does not transfer to a human project, is
> disclosed in the README under "How this was built".
>
> Repo: https://github.com/Stoicera/migration-lab-java · modern stand:
> https://migration-lab.stoicera.cyou (synthetic data, public visitors can change it). Apache-2.0.
>
> I would especially like to hear from people who have done a Boot 1.x or AngularJS migration
> for real: what broke for you that a suite like this would not have caught?

**Before posting, update the "still rough" paragraph.** If the Tomcat findings are fixed by then,
delete the sentence; if the Dependabot count changed, use the new number with its date.

## Prepared answers (use only if asked, in your own words)

- **"Why synthetic and not a real legacy app?"** No suitable, genuinely abandoned, permissively
  licensed application existed; the search is documented in ADR-0001. The weaknesses are
  catalogued in `legacy/LEGACY_NOTES.md`.
- **"Was this built with AI?"** Answer openly and point to the README section "How this was
  built". Do not make it the headline and do not quote hours or a price from it (case study 18
  §9.4 and §9.10).
- **"Can I see the legacy stand live?"** It preserves an SQL injection on purpose, so it is
  behind Basic auth; `docker compose -f legacy/docker-compose.yml up -d --wait` runs it locally.
  Credentials are shared on request, never in the thread.
- **"Why not OpenRewrite for everything?"** It was used and evaluated: ADR-0002 and playbook
  chapter 4 list what it carried and where manual work started.
- **"Why not microservices?"** Out of scope by decision (PRD §5); staying modular is the answer.
- **"73 % mutation score means AI tests are …"** No general claim from six classes. "In our
  experiment" with the date, and the link to `ai-testgen/REPORT.md`.

## Do not

- Post the legacy credentials, server names or exploit inputs for the preserved SQL injection.
- Show or link a screenshot of the JSP admin page.
- Generalise any number from the AI experiment beyond "in our experiment".
- Quote effort hours.
