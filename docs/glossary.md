# Glossar / Glossary

Deutsche Fachbegriffe (Playbook, Entscheider-Kommunikation) ↔ English terms used in
code and technical docs. Grows with the playbook chapters.

| Deutsch | English | Meaning |
|---|---|---|
| Sicherheitsnetz | safety net | Selenium E2E suite + characterization tests that must stay green from `stage-1-safety-net` onward; defines migration risk control |
| Charakterisierungs-Test | characterization test | Golden-master capture of *actual* current behaviour (including quirks) — defines functional equivalence, not correctness |
| Würgefeigen-Strategie | Strangler Fig | Incremental replacement: new Angular shell absorbs the AngularJS app route by route while both coexist |
| Mutationstest | mutation testing (PIT) | Quality measure for tests: how many seeded code mutations does the suite detect ("kill")? Honest counterpart to bare coverage |
| Etappe | stage | A tagged, checkout-and-run repository state (see `stages.md`) |
| Altsystem / Bestandssystem | legacy system | The system as found — here: WerkstattCRM in `legacy/` |
| Golden Master | golden master | Committed reference capture of a response/state; any diff is a finding. Governance: ADR-0007 |
| Selektor-Landkarte | selector map | Per-UI properties file mapping intent keys → CSS selectors; lets the same E2E scenarios drive old and new UI |
| Wire-Format / API-Vertrag | wire format / API contract | The exact serialized shape of API responses; part of functional equivalence, pinned by config (ADR-0005) |
| Feld- vs. Konstruktor-Injektion | field vs. constructor injection | `@Autowired` on fields (untestable 2016 style) vs. dependencies via constructor (testable; stage-4 sweep) |
| Sanktionierte Abweichung | sanctioned divergence | A deliberate, registered behaviour difference between legacy and modern (register: ADR-0004) — everything else is a regression |
| Aufwands-Etikettierung | effort labelling | Measured agent wall-time vs. experience-based field estimates — never mixed, always labelled (playbook rule) |
| OpenRewrite-Rezept | OpenRewrite recipe | Automated migration transformation; used supervised, evaluated honestly (ADR-0002) |
