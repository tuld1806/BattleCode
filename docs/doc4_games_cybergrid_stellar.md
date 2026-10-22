# ALGORITHMIC PEDAGOGY & GAME PLUG-IN SPECIFICATIONS
## `Cyber-Grid: Neon Dominion` & `Stellar Armada: Vector Protocols`

This document details the game rules, mathematical models, physics equations, and algorithmic progression for the two official arena plug-ins.

---

## PART I: ALGORITHMIC PEDAGOGY MATRIX

The two games form a complementary curriculum for first-year undergraduate students:
1. **Cyber-Grid: Neon Dominion:** Exercises **Discrete Mathematics, 2D Arrays & Graph Algorithms** ($BFS, A^*$, Multi-Agent Pathfinding, Fog of War memory maps).
2. **Stellar Armada: Vector Protocols:** Exercises **Continuous Mathematics, Computational Geometry & Physics-based Control** (Newtonian kinematics, Euler numerical integration, lead-target ballistics, PID / steering behaviors).

| Mechanics | Target Algorithmic Skills | Difficulty Level |
| :--- | :--- | :--- |
| **Cyber-Grid:** Navigation around obstacles | Matrix graph traversal; Breadth-First Search (**BFS**), **Dijkstra**, and **$A^*$ (A-Star)** shortest path. | Beginner $\rightarrow$ Intermediate |
| **Cyber-Grid:** Unit collision penalty (-10 HP) | **Multi-Agent Pathfinding (MAPF)**; reservation table conflict resolution. | Intermediate |
| **Cyber-Grid:** Limited Vision (Fog of War) | **Frontier Exploration**; spatial memory map persisting known nodes. | Intermediate |
| **Cyber-Grid:** Unit spawning & Node economics | Greedy algorithms; Finite State Machines (**FSM**); resource optimization. | Beginner $\rightarrow$ Intermediate |
| **Stellar Armada:** Inertia & Drag | **Euler Numerical Integration**; **Arrive/Steering behaviors** to brake accurately near asteroids without drifting. | Intermediate $\rightarrow$ Advanced |
| **Stellar Armada:** Lead-target torpedo fire | Quadratic equations solving intercept points between moving ship vector and missile velocity. | Advanced |
| **Stellar Armada:** Momentum collision ($\Delta V$) | **Local collision avoidance** (Velocity Obstacles / Boids separation) or kamikaze ramming tactics. | Intermediate $\rightarrow$ Advanced |

### Platform Access & Testing Routes (Synchronized with `docs/thiet_ke_giao_dien.md`)
Students iterate, test algorithms, and analyze telemetry for both games through dedicated sub-routes:

| Platform Feature | Cyber-Grid Route | Stellar Armada Route | Purpose |
| :--- | :--- | :--- | :--- |
| **Rules & Starter Kits** | `/cyber-grid/overview` | `/stellar-armada/overview` | Review algorithmic rules & download starter kits (Python, C++, Java). |
| **24/7 Practice Sparring** | `/cyber-grid/practice` | `/stellar-armada/practice` | Test bot logic 24/7 against `DummyBot`, `EasyBot`, and `HardBot`. |
| **Bot Code Submission** | `/cyber-grid/submission` | `/stellar-armada/submission` | Push Git code, inspect smoke tests, and set active practice bot. |
| **Peer Scrimmage Arena** | `/cyber-grid/scrimmage` | `/stellar-armada/scrimmage` | Direct peer challenge or Elo-based matchmaking. |
| **Scrimmage Leaderboard** | `/cyber-grid/scrimmage-leaderboard` | `/stellar-armada/scrimmage-leaderboard` | View informal rankings, inspect player profiles & replays. |
| **Replay & Telemetry** | `/replay/[matchId]` | `/replay/[matchId]` | 60fps canvas replay with Fog of War toggle & `TickInspector`. |

---

## PART II: PLUG-IN 1 — `CYBER-GRID: NEON DOMINION`

### 1. Grid Coordinates & Distance Standards
- **Grid Coordinates:** $(x, y)$ where $x \in [0, N-1]$ (columns, West to East), $y \in [0, N-1]$ (rows, North to South).
- **Orthogonal Directions:** `NORTH`: $(x, y-1)$, `SOUTH`: $(x, y+1)$, `EAST`: $(x+1, y)$, `WEST`: $(x-1, y)$.
- **Manhattan Distance:**
  $$d(A, B) = |x_A - x_B| + |y_A - y_B|$$
- **Line of Sight (Bresenham's Algorithm):** Ranged attacks from `BLASTER` and `TOWER` cast a ray from $(x_1 + 0.5, y_1 + 0.5)$ to $(x_2 + 0.5, y_2 + 0.5)$. If any intermediate grid cell contains an `OBSTACLE`, the attack is blocked.

### 2. Entity Specifications
| Entity (`type`) | Max HP (`max_hp`) | Attack (`atk`) | Range | Vision | Cost (`cost`) | Special Ability |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **`CORE`** | $1,000$ | $30$ | $3$ | $6$ | — | Immobile. Generates $+10$ Data/turn. Can `SPAWN` `WORKER`, `PAWN`, `BLASTER` in adjacent cells. |
| **`WORKER`** | $50$ | $5$ | $1$ | $5$ | $50$ | Mines `RESOURCE_NODE` while stationed on it. Can `BUILD_TOWER` in adjacent cell. |
| **`PAWN`** | $120$ | $25$ | $1$ | $3$ | $100$ | Primary melee frontline unit. |
| **`BLASTER`** | $80$ | $35$ | $3$ | $4$ | $150$ | Ranged sniper; projectile blocked by `OBSTACLE`. |
| **`TOWER`** | $300$ | $40$ | $4$ | $4$ | $200$ | Defensive turret constructed by `WORKER`. Immobile. |

### 3. Deterministic Four-Phase Tick Resolution
Each unit executes at most 1 action per tick.
1. **Phase 1 — Spawn & Build (`SPAWN` / `BUILD`):** `CORE` spawns mobile units; `WORKER` builds `TOWER`. Target cell must be unblocked. New units activate at tick $t + 1$.
2. **Phase 2 — Movement & Collision Resolution (`MOVEMENT`):**
   - Out of bounds or walking into `OBSTACLE`/`CORE`/`TOWER`: unit stays in place, suffers $10$ collision damage.
   - **Multi-Unit Collision:** If $\ge 2$ units move to the same cell, or enter an occupied cell, or swap cells ($(x_1, y_1) \leftrightarrow (x_2, y_2)$): all involved units bounce back to their origin and take $10$ collision damage.
3. **Phase 3 — Simultaneous Combat (`COMBAT`):** Attacks within range and clear line of sight resolve simultaneously. Accumulated damage is deducted at the end of Phase 3. Units with $\text{HP} \le 0$ are removed.
4. **Phase 4 — Economy & Harvesting (`ECONOMY`):**
   $$\text{Resources}_T(t) = \text{Resources}_T(t-1) + 10 + 5 \times (\text{Active Mined Nodes})$$

---

## PART III: PLUG-IN 2 — `STELLAR ARMADA: VECTOR PROTOCOLS`

Continuous 2D plane $[0.0, 1000.0] \times [0.0, 1000.0]$ governed by Newtonian physics.

### 1. Ship Classes & Physical Attributes
| Ship (`type`) | HP | Mass ($M$) | Hit Radius ($R_{\text{hit}}$) | Max Thrust ($F_{\max}$) | Max Turn Rate ($\omega_{\max}$) | Ore Cost | Special Ability |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **`FLAGSHIP`** | $2,000$ | $500.0$ | $25.0$ | $10.0\text{ N}$ | $0.05\text{ rad/tick}$ | — | Fleet command. Destruction triggers immediate defeat. |
| **`FIGHTER`** | $100$ | $20.0$ | $6.0$ | $15.0\text{ N}$ | $0.25\text{ rad/tick}$ | $100$ | **Instant Laser:** Range $80.0$, $15$ dmg/tick, zero cooldown. Intercepts torpedoes. |
| **`MINER`** | $150$ | $50.0$ | $10.0$ | $5.0\text{ N}$ | $0.15\text{ rad/tick}$ | $120$ | Extracts $+5\text{ Ore/tick}$ within $<30.0$ of Asteroid at speed $<2.0$. Capacity $100$. |
| **`TORPEDO_FRIGATE`** | $400$ | $150.0$ | $14.0$ | $4.0\text{ N}$ | $0.10\text{ rad/tick}$ | $250$ | **Heavy Torpedo:** Range $300.0$, $120$ dmg, cooldown $25$ ticks. |

### 2. Five-Step Physics Engine Pipeline (Each Tick)
Calculated with IEEE 754 precision rounded to 6 decimal places internally:

1. **Step 1 — Heading Rotation:**
   $$\Delta \theta = \text{atan2}(\sin(\theta_{\text{target}} - \theta_{\text{old}}), \cos(\theta_{\text{target}} - \theta_{\text{old}}))$$
   $$\theta_{\text{new}} = \theta_{\text{old}} + \text{clamp}(\Delta \theta, -\omega_{\max}, +\omega_{\max})$$
2. **Step 2 — Kinematics & Drag:**
   $$\vec{F} = (\text{clamp}(\text{thrust}, 0, F_{\max}) \cdot \cos\theta_{\text{new}}, \text{clamp}(\text{thrust}, 0, F_{\max}) \cdot \sin\theta_{\text{new}})$$
   $$\vec{V}_{\text{new}} = \vec{V}_{\text{old}} \times (1 - 0.02) + \frac{\vec{F}}{M}$$
   $$\vec{P}_{\text{new}} = \vec{P}_{\text{old}} + \vec{V}_{\text{new}}$$
   - Boundary Reflection: If $x \notin [0, 1000]$ or $y \notin [0, 1000]$, velocity reverses normal component with restitution $e = 0.5$.
3. **Step 3 — Collision Resolution:**
   When distance $d(A, B) < R_{\text{hit}, A} + R_{\text{hit}, B}$ with relative velocity $\Delta V = \|\vec{V}_A - \vec{V}_B\|$:
   $$\text{Damage}_A = 0.05 \times M_B \times |\Delta V|^2$$
   $$\text{Damage}_B = 0.05 \times M_A \times |\Delta V|^2$$
   Positions are separated along the contact normal.
4. **Step 4 — Ballistics & Weapons:**
   - `FIGHTER` Laser: Instant hit within range $80.0$ dealing $15$ damage; can target enemy `TORPEDO` ($\text{HP} = 15$).
   - `TORPEDO` Kinematics: Accelerates over first 10 ticks to $V_{\max} = 12.0/\text{tick}$. Detonates on impact or at range limit $300.0$ dealing $120$ AoE damage.
5. **Step 5 — Mining & Match Victory:**
   - `MINER` harvests $+5$ Ore/tick when within distance $<30.0$ of Asteroid and speed $<2.0$. Offloads into team bank when within $<50.0$ of `FLAGSHIP`.
   - Victory evaluated after $1,200$ ticks or upon destruction of enemy `FLAGSHIP`.