# Reddit — drafts (EN)

> **Status: DRAFT (2026-10-15). Not posted.** The founder posts from his own account, one sub at a
> time, and answers every comment.

## Before anything else: read each sub's rules

The rules and sidebars of the three subs **could not be fetched on 2026-10-15** (reddit.com
blocks requests from this environment). The notes per sub below are therefore marked
*unverified* and must be checked by hand before posting: rules page, sidebar, pinned posts, and
the flair list. If a rule forbids the post, do not post it there.

Reddit-wide: self-promotion is tolerated when it is a small part of what the account does and the
post is useful on its own. Post from an account that also comments elsewhere. Say plainly that it
is our project.

---

## r/java

**Angle:** the jump path and what broke. Technical, no pitch.

*Unverified rule notes:* r/java is for Java news and technical discussion; help questions go to
r/javahelp. Check whether project showcases need a flair or are limited to a weekly thread.

**Title:** `Spring Boot 1.5 → 2.7 → 3.5 → 4.1 and Java 8 → 25 on one app, with the same tests run against both versions (open source)`

**Body:**

> We migrated a small 2016-style Spring Boot 1.5 / AngularJS app to Java 25 / Spring Boot 4.1 /
> Angular 22 in public, in six tagged stages, and kept the old stand running next to the new one
> so the same suites can run against both. Repo (Apache-2.0):
> https://github.com/Stoicera/migration-lab-java
>
> Some things that might be useful if you have a Boot 1.x app of your own:
>
> - **We went through 2.7 instead of jumping.** Boot 1.5.22 → 2.7.18 + Java 17 was one documented
>   jump with three real breaks; then 2.7 → 3.5 → 4.1 + Java 25.
> - **The break you will not see in the UI:** in the 1.5 → 2.7 jump the JSON serialisation of
>   `java.sql.Date` changed. The screens looked right; the wire contract had changed. A
>   characterization test (golden master over the HTTP response) caught it (2026-07-30).
> - **Flyway in Boot 4:** `flyway-core` alone migrated nothing; the app started and died later
>   on a missing table. `spring-boot-starter-flyway` is the piece that wires it (2026-08-05).
> - **OpenRewrite** was used and evaluated, not trusted blindly: ADR-0002 and playbook chapter 4
>   list what the recipes carried and where manual work started.
> - **Readiness did not include the database by default.** With the DB stopped, readiness said
>   `200 UP` while `/actuator/health` said 503. After the fix the container goes unhealthy 25 s
>   after the DB dies and healthy 6 s after it returns (measured 2026-08-05).
>
> The safety net: 47 characterization tests and 34 Selenium scenarios, green against both stands
> in CI (latest runs on master, 2026-10-15).
>
> What it is not: a tool you point at your codebase. The app is about 1.7k lines of backend on
> purpose; the numbers do not scale linearly. Not faster either: our local read-path load test
> showed no consistent difference (two runs, 2026-08-05 and 2026-10-02, median and p95 point in
> different directions).
>
> Happy to answer questions on any of the stages. Which break hurt most in your own Boot 1.x → 3.x
> migrations?

---

## r/programming

**Angle:** a finding about measurement, not the project. Link post to the article (devto.md),
once it is published, rather than to the repository.

*Unverified rule notes:* r/programming has historically removed "I made this" / project
showcase posts and low-effort content; a technical article usually fits better than a repo link.
Check the current rules on AI-related content and on self-posts before submitting.

**Title (link to the article):** `Our LLM-generated tests scored 99 % mutation score, until we counted the ones that did not compile`

**First comment by the author:**

> Author here. Short version: we generated unit tests for the same six classes twice (as 2016
> Java 8 code and as their migrated Java 25 counterparts) with two models, under a protocol
> frozen before the first API call (git tag `ai-testgen-protocol-v1`, 2026-07-31). As generated,
> 12 of 24 test classes compiled, and over those 12 the numbers looked perfect: 100 % line
> coverage, 99.2 % mutation score. After a time-boxed repair pass brought 21 of 24 to green, the
> same metrics were 90.5 % line and 73.2 % mutation (2026-08-02). The hard classes had simply not
> been in the denominator.
>
> Two more results: the largest class reached 84.3 % line coverage but only 55.9 % mutation score,
> so an 80 % coverage gate would have passed it; and across 421 repaired test methods, none found
> a real bug. Generated tests pinned existing behaviour, which is what you want in a migration,
> but they did not audit it.
>
> This is six classes per codebase, one attempt each. It is an illustration, not a study, and the
> report says so. Protocol, raw outputs, fix log and PIT reports:
> https://github.com/Stoicera/migration-lab-java/tree/master/ai-testgen

---

## r/selfhosted

**Fit, honestly:** weak. migration-lab is not a service anyone would self-host. Only one part is
useful to this audience: the operational traps from stage 6. Post only that angle, or skip this
sub.

*Unverified rule notes:* r/selfhosted has had rules limiting new-project posts to a specific day
and requiring disclosure of AI involvement in projects. This draft is not a project
announcement, but check both before posting.

**Title:** `postgres:18-alpine reports en_US.utf8 and sorts in C order anyway — two Postgres/Docker traps we hit`

**Body:**

> Two things we measured while moving a small Spring Boot app from PostgreSQL 9.6 to 18 in Docker
> (2026-08-05), in case they save someone an evening:
>
> 1. **Collation on Alpine.** `postgres:18-alpine` reports `datcollate=en_US.utf8`, and sorts in C
>    order anyway: musl accepts the locale name and ignores it. A check that reads the setting
>    passes while every sorted list silently changes order. Same seven names, measured: on
>    9.6 and on `postgres:18` (Debian) the list starts with "de Vries"; on `18-alpine`,
>    "de Vries" lands behind "Zach" and "Öhler" at the very end. We found it by sorting
>    the same names on each image instead of reading the setting. We now run `postgres:18` (Debian),
>    and there is a test that sorts instead of asking.
> 2. **Health that tells the truth.** Spring Boot's default readiness group did not include the
>    database: with the DB stopped, readiness answered `200 UP` while `/actuator/health` answered
>    503. After fixing the group and the retry budget, the container goes unhealthy 25 s after the
>    database dies and healthy again 6 s after it returns. Liveness still ignores the database on
>    purpose, so a DB outage does not restart a healthy app.
>
> Since 2026-09-23 both containers also carry `restart: unless-stopped` and hard memory limits;
> before that, a host reboot left them down until someone redeployed.
>
> Full write-up with the compose files (our project, Apache-2.0):
> https://github.com/Stoicera/migration-lab-java (playbook chapter 7 is in German; the README and
> `docs/deployment.md` are in English).

*Source of the sort example:* playbook chapter 7, table "Falle 1" (measured 2026-08-05).

---

## Do not (all subs)

- Post the legacy credentials, server names or exploit inputs for the preserved SQL injection.
- Link or show the JSP admin page screenshot.
- Generalise any AI-experiment number beyond "in our experiment".
- Quote effort hours.
- Cross-post the same text to several subs on the same day; one sub, one angle.
