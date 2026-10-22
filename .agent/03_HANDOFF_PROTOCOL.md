# AUTONOMOUS AGENT HANDOFF & CONTINUOUS CONTEXT PROTOCOL
**File:** `.agent/03_HANDOFF_PROTOCOL.md`  
**Target:** BattleCode-UET Autonomous Engineering Pipeline  
**Governing Standard:** Superpowers Skill Suite & Zero-Interruption Protocol

---

## 1. Core Mission: Zero-Waste Context Architecture

In complex multi-phase software projects, reading the entire codebase (dozens of Java classes, SQL migrations, Next.js components, configuration files) consumes 40,000–80,000 context tokens per turn, degrades reasoning accuracy, and slows down execution.

**The Continuous Context Protocol solves this permanently:**
Every autonomous session documents its own progress systematically. Instead of scanning the repository, **any incoming agent session boots up in under 2,000 tokens** by ingesting exactly 4 standardized control files:

```
┌────────────────────────────────────────────────────────┐
│             INCOMING AGENT FAST-BOOT (2,000 tokens)    │
│  1. .agent/00_CONSTITUTION.md    (Immutable Invariants)│
│  2. .agent/01_PROJECT_STATE.md   (Single Source Truth) │
│  3. .agent/02_PHASE_ROADMAP.md   (Active Phase Tasks)  │
│  4. .agent/handoffs/latest.md    (Last Session Ledger) │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
           [EXECUTES PHASE TASKS VIA TDD]
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             OUTGOING AGENT HANDOFF (4 Steps)           │
│  1. Run Terminal Verification (Get fresh proof)        │
│  2. Update .agent/01_PROJECT_STATE.md                  │
│  3. Generate .agent/handoffs/phase-[N]-handoff.md       │
│  4. Clean Git Commit                                   │
└────────────────────────────────────────────────────────┘
```

---

## 2. Fast-Boot Protocol for Incoming Agents

When an agent begins a new conversation or takes over an active phase:

1. **DO NOT run broad directory searches or view arbitrary source files.**
2. **Read ONLY the four control files in order:**
   - [00_CONSTITUTION.md](file:///c:/Users/admin/Downloads/BattleCode/.agent/00_CONSTITUTION.md) (absorb Section 3 Hard Invariants).
   - [01_PROJECT_STATE.md](file:///c:/Users/admin/Downloads/BattleCode/.agent/01_PROJECT_STATE.md) (absorb verified subsystems and active phase).
   - [02_PHASE_ROADMAP.md](file:///c:/Users/admin/Downloads/BattleCode/.agent/02_PHASE_ROADMAP.md) (locate the specific tasks of the active phase).
   - The most recent ledger in `.agent/handoffs/` (absorb recent rulings and exact next step).
3. **Begin execution immediately** on the first pending task using the `executing-plans` and `test-driven-development` skills.

---

## 3. The 4-Step Mandatory Handoff Sequence

Upon completing all tasks in a phase (or before yielding context when a session reaches completion boundaries), the agent **MUST autonomously execute all 4 steps without asking for user permission**:

### Step 1: Real Terminal Verification Gate
**REQUIRED SUB-SKILL:** `superpowers:verification-before-completion`.
- Execute the real build and test command for the phase:
  - For Backend / Engine: `mvn test`
  - For Frontend: `cd frontend && npm run typecheck && npm run build`
  - For Docker / Scripts: `docker build ...` or test script execution.
- Capture:
  - Exit code (must be `0`).
  - Total tests run, failures, and execution time.
- **Strict Rule:** Never write a handoff claiming completion without fresh command output from the current turn.

### Step 2: Live State Synchronization
Update [01_PROJECT_STATE.md](file:///c:/Users/admin/Downloads/BattleCode/.agent/01_PROJECT_STATE.md):
- Mark completed deliverables as `COMPLETE` in Section 1.
- Update total verified test counts in the summary header.
- Add any newly established interfaces, DTOs, or database tables to Section 2.
- Advance the `Active Phase` pointer (`PHASE_N` $\rightarrow$ `PHASE_N+1`).

### Step 3: Emit Standardized 20-Line Handoff Ledger
Generate a new handoff document in `.agent/handoffs/phase-[N]-handoff.md` strictly following the canonical template in Section 4 below.

### Step 4: Clean Git Synchronization
Run:
```bash
git add .
git commit -m "feat(phase-N): [Phase Title] completed with verified tests"
```

---

## 4. Canonical Handoff Ledger Template (`phase-[N]-handoff.md`)

Every file written to `.agent/handoffs/phase-[N]-handoff.md` must strictly adhere to this compact, high-density format:

```markdown
# Autonomous Phase Handoff Ledger — Phase [N]: [Phase Title]
**Timestamp:** YYYY-MM-DDTHH:MM:SS+07:00
**Active Phase Advanced To:** Phase [N+1]: [Next Title]
**Commit SHA:** [Git SHA or current HEAD]

### 1. Verification Evidence
- Command: `[Exact terminal command run]`
- Result: `[Exit Code 0, X/X tests pass, 0 failures, 0 errors, Time: Y.Ys]`
- Verified Artifacts: `[e.g., target/classes, frontend/out, etc.]`

### 2. Delivered Components & Contracts
- `[file/path/A.ts]`: [Exact responsibility / exported interface]
- `[file/path/B.tsx]`: [Exact responsibility / exported interface]
- `[file/path/C.sql]`: [Exact table or constraint added]

### 3. Key Technical Decisions & Rulings
- Ruling 1: [Specific decision made to conform with constitution/spec]
- Ruling 2: [Library or algorithm choice made to prevent performance bottleneck]

### 4. Immediate Next Step for Incoming Agent
- Target: Phase [N+1], Task [X.1] in `.agent/02_PHASE_ROADMAP.md`
- First Action: [Exact test file to create and failing test to write]
```

---

## 5. Anti-Patterns & Prohibitions

| Prohibited Anti-Pattern | Constitutional Correction |
| :--- | :--- |
| Asking *"Should I write the handoff now?"* | Write the handoff immediately upon completing the phase tasks. |
| Writing a handoff without running terminal tests | Run `mvn test` or `npm run build` first and embed real evidence. |
| Overwriting previous handoff ledgers | Always create a new file `phase-[N]-handoff.md` to preserve historical trajectory. |
| Dumping raw code into handoff documents | Document file paths, interface signatures, and rulings only. |
| Leaving uncommitted Git changes at handoff | Always stage and commit changes before declaring the phase complete. |
