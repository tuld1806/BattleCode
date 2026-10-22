# SDD ledger — plan: docs/superpowers/plans/2026-10-23-battlecode-uet-pilot.md

- Baseline: existing engine had 6 passing JUnit tests. Maven dependencies required approved network access; local repository is `.m2/repository`.
- Ruling: work in the current checkout because the repository has no initial commit and `.git` is read-only in this environment; source changes and test evidence remain in the workspace. Cost if wrong: integration will need a later commit in a writable Git checkout.
- Task 1: complete (tests: `mvn '-Dmaven.repo.local=C:/Users/admin/Downloads/BattleCode/.m2/repository' -f backend/pom.xml test` → 11/11 pass). Added fog, collision, combat, malformed duplicate, missing action, and score tests; corrected action canonicalization.
- Task 2: local implementation complete (tests: full Maven suite → 20/20 pass). Persistent bot I/O, time bank, output/debug limits, fail-closed Linux sandbox path, and reconstructable gzip replay. Official sandbox isolation and 96 MB Java runtime remain unverified until an actual Linux worker VM is available.
- Next: Task 3 tournament persistence and API.
