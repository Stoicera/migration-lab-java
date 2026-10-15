# Launch checklist — migration-lab

> **Status: DRAFT (2026-10-15).** Derived from the Stoicera prompt `ai/prompts/oss-launch.md`
> (SSOT). Nothing in `docs/launch/` has been posted, submitted or scheduled. The founders post and
> answer; agents prepare.

Goal: core/26 Ziel 7 (1,000 GitHub stars across the Stoicera repositories by 2027-07-07). This
repository had 0 stars on 2026-10-15 (`gh repo view`).

## A. Before launch day (repository)

- [ ] **Social preview uploaded.** Settings → General → Social preview → Edit → Upload
      `docs/assets/social-preview.png` (1280×640). Regenerate first if the text changed:
      `PLAYWRIGHT_CORE_PATH=<folder with playwright-core> node scripts/social-preview.mjs`.
- [ ] **Homepage field of the repository** points to the page meant to rank,
      `https://stoicera.com/werk/migration-lab-java/`, instead of the demo (the demo is noindex
      since 2026-10-15, #95): `gh repo edit Stoicera/migration-lab-java --homepage https://stoicera.com/werk/migration-lab-java/`.
- [ ] **Trivy CRITICALs decided.** On 2026-10-15 the modern image has 3 CRITICAL findings in
      embedded Tomcat (run 38686720064). Either pin Tomcat ≥ 11.0.25 before launch, or keep the
      sentence in the Show HN comment that names it. Do not launch with the problem unmentioned.
- [ ] **Dependabot queue** reduced or named: 14 open PRs on 2026-10-15.
- [ ] **Still missing from `oss-launch.md` Part A:** `CONTRIBUTING.md`, `.github/ISSUE_TEMPLATE/`
      (bug, feature, question), a pull-request template, `llms.txt`. Not in this PR.
- [ ] **Stranger test:** clone fresh on a second machine and run the README quickstart; note the
      time of the first `docker compose up` (not measured for these drafts).

## B. On the morning of launch day (30 minutes)

- [ ] Demo answers: `curl -s -o /dev/null -w '%{http_code}\n' https://migration-lab.stoicera.cyou/kunden`
      → 200 (it was 200 on 2026-10-15).
- [ ] Latest `legacy-ci`, `modern-ci` and `e2e` runs on `master` are green; if a newer run than
      2026-10-15 exists, update the run links in the README block "What you can verify in 5
      minutes" and the dates in the drafts.
- [ ] Re-count open Dependabot PRs and Trivy findings; update the "still rough" paragraph in
      `show-hn.md`.
- [ ] Read each subreddit's rules and sidebar by hand (`reddit.md` — they could not be fetched on
      2026-10-15).
- [ ] Every draft read aloud once by its poster; anything that does not sound like you, change.

## C. Order of posting — all on the same day

| Time (Vienna) | Channel | Who posts | Who answers |
|---|---|---|---|
| 15:00 | Hacker News, Show HN (`show-hn.md`) | Sebastian | Sebastian, all day |
| 15:30 | LinkedIn, post 1 (`linkedin.md`), link in first comment | Sebastian | Sebastian |
| 16:00 | r/java (`reddit.md`) | Sebastian | Sebastian |
| 17:00 | LinkedIn, post 2 (`linkedin.md`) | Raphael | Raphael; technical questions to Sebastian |
| next day | dev.to article (`devto.md`), then r/programming with the article link | Sebastian | Sebastian |
| only if it fits after reading the rules | r/selfhosted (`reddit.md`, ops angle only) | Sebastian | Sebastian |

The 15:00 slot is a convention (US East Coast morning), not a rule; HN has no official best time.
The dev.to/r/programming pair moves to the next day because the article must be published before
it can be linked; everything that points to the repository goes out on launch day.

## D. The six hours after posting

- [ ] The author answers **every** comment on HN and Reddit within the first six hours, in his
      own words: questions first, criticism second, thanks last.
- [ ] No upvote requests anywhere, no second accounts, no asking colleagues to comment
      (HN guidelines; Reddit site-wide rules).
- [ ] Criticism that is right becomes a GitHub issue the same day, linked in the reply.
- [ ] Prepared answers in `show-hn.md` are a crib, not copy-paste text.

## E. Afterwards

- [ ] Day 1 and day 7: stars, forks, clones, traffic sources (`gh api repos/Stoicera/migration-lab-java/traffic/views`
      and `.../traffic/popular/referrers`), noted with date in the SSOT.
- [ ] One line in `ops/runlog.md` (SSOT) per channel posted: date, channel, link.

## Binding block list (case study 18 §9) — check every text against it

- No legacy-stand credentials, no server IPs or host names, no panel URLs.
- No effort hours, no "migration in X hours/days", nothing that reads as a price.
- No "AI-built" framing; the method is answered openly when asked (README "How this was built").
- No general claims from the AI experiment (N = 6 classes per codebase): "in our experiment",
  with date.
- Never the JSP admin page screenshot; no exploit inputs for the preserved SQL injection.
- Backups: no "second location", "outside the data centre" or "geo-redundant".
- No superlatives, no religious vocabulary.
