# Pacman

## Goal
Design a Pacman agent that **maximizes score while minimizing death risk**, using only local information and fast computations suitable for real-time play.

---

## Core Design Choices

### 1. Grid as a Graph
The board is modeled as a graph where each free cell is a node and edges connect adjacent cells.
**Why:** Enables shortest-path algorithms (BFS/A*) and clean distance reasoning.

---

### 2. Distance-First Thinking (BFS)
At every time step:
- Run BFS from Pacman
- Run BFS from each ghost

This gives exact shortest-path distances.
**Why:** Distances are the most reliable signal for danger, reachability, and optimal movement in Pacman.

---

### 3. Finite State Machine (FSM)
Pacman is always in one clear state:
- **CHASE** – eat vulnerable ghosts
- **ESCAPE** – survive immediate danger
- **GET_POWER_PELLET** – turn danger into opportunity
- **EAT_DOTS** – score efficiently

**Why:** FSM makes behavior interpretable, debuggable, and prevents conflicting goals.

---

## State Selection Logic

Priority is strictly ordered:
1. If a ghost is vulnerable → **CHASE** (highest reward)
2. If a ghost is very close → **ESCAPE** (survival first)
3. If danger is near but manageable → **GET_POWER_PELLET**
4. Otherwise → **EAT_DOTS**

**Why:** This ordering matches optimal human Pacman play: reward only matters after survival.

---

## Danger Modeling

Each cell has a danger value based on proximity to normal ghosts.
Closer ghosts = higher danger.

**Why:** Avoids greedy behavior that walks into traps or dead ends.

---

## Target Selection Strategy

All targets (dots, pellets, ghosts) are chosen using the same idea:

```
cost = distance + danger − reward
```

- Distance: prefer closer targets
- Danger: avoid risky paths
- Reward: encourage high-value actions

**Why:** A single cost function keeps behavior consistent across states.

---

## ESCAPE Behavior

When escaping, Pacman does **not** plan long paths.
Instead, it chooses the neighboring cell that maximizes the minimum distance to any ghost.

**Why:** In emergencies, local safety is more reliable than long-term planning.

---

## Movement Execution

Pacman:
1. Chooses a target (or safe neighbor)
2. Computes a shortest path
3. Executes **only one step**
4. Re-evaluates next turn

**Why:** Continuous re-planning allows fast reaction to ghost movement.
