# Pacman Game Server

## Overview

The `server` package contains all the server-side components for the Pacman game. It manages game state, physics, ghost AI, rendering, and audio. The server is designed to be independent from the client-side algorithm, allowing different AI implementations to control Pacman.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Class Documentation](#class-documentation)
3. [Game Flow](#game-flow)
4. [Ghost AI System](#ghost-ai-system)
5. [Rendering System](#rendering-system)
6. [Audio System](#audio-system)
7. [Coordinate System](#coordinate-system)
8. [Configuration](#configuration)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         GAME SERVER                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────┐  │
│  │    Game     │───►│  GameState  │◄───│      GhostImpl[]        │  │
│  │  (Main)     │    │  (Data)     │    │  (Ghost Entities)       │  │
│  └──────┬──────┘    └─────────────┘    └─────────────────────────┘  │
│         │                                                            │
│         ├───────────────┬───────────────┬───────────────────────────┤
│         │               │               │                           │
│         ▼               ▼               ▼                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                   │
│  │   GameMap   │ │AudioManager │ │   StdDraw   │                   │
│  │ (Pathfind)  │ │  (Sound)    │ │ (Graphics)  │                   │
│  └─────────────┘ └─────────────┘ └─────────────┘                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  PacmanGame     │
                    │  (Interface)    │
                    └─────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   Ex3Algo       │
                    │  (Client AI)    │
                    └─────────────────┘
```

---

## Class Documentation

### Game.java

**Purpose:** Main game controller that orchestrates all game components.

**Responsibilities:**
- Initialize game state and GUI
- Process player/AI moves
- Update game physics
- Manage ghost AI
- Handle collisions
- Trigger audio events
- Render game state

**Key Constants:**
```java
DOT_SCORE = 10              // Points for eating a dot
POWER_PELLET_SCORE = 50     // Points for eating a power pellet
GHOST_SCORE = 200           // Points for eating a ghost
GHOST_START_DELAY = 50      // Moves before ghosts start moving
GHOST_MOVE_PROBABILITY = 0.3 // Random movement probability
GHOST_START_X/Y = 11        // Ghost spawn position
```

**Key Methods:**

| Method | Description |
|--------|-------------|
| `init(scenario, id, cyclic, seed, resolution, dt, unused)` | Initialize game with parameters |
| `move(int dir)` | Process a movement command |
| `play()` | Start/resume the game |
| `end(int code)` | End the game |
| `getGame(int code)` | Get the board as 2D array |
| `getPos(int code)` | Get Pacman's position as "x,y" |
| `getGhosts(int code)` | Get array of ghost objects |
| `getStatus()` | Get current game status |

**Initialization Flow:**
```
init()
  ├── Create GameState
  ├── Create Random (with seed)
  ├── Store parameters (dt, scenario)
  ├── initializeGame()
  │     ├── Set Pacman position (14, 11)
  │     └── Create 4 ghosts at (11, 11)
  ├── initializeGUI()
  │     ├── Set canvas size (720x720)
  │     ├── Configure coordinate system
  │     └── Enable double buffering
  ├── Set status to PAUSED
  ├── waitForSpaceKey()
  └── play()
```

---

### GameState.java

**Purpose:** Central data store for all game state information.

**Fields:**
```java
int[][] board          // The game board (22x23)
int pacmanX, pacmanY   // Pacman's position
List<GhostImpl> ghosts // All ghost entities
int score              // Current score
int status             // Game status (NOT_STARTED, RUNNING, PAUSED, DONE)
boolean cyclicMode     // Whether map wraps around
double sharedVulnerableTime // Shared vulnerability timer for all ghosts
int dt                 // Delay time between moves
```

**Cell Type Constants:**
```java
EMPTY = 0          // Empty walkable space
WALL = -1          // Obstacle (blue)
DOT = 1            // Regular dot (pink, 10 points)
POWER_PELLET = 2   // Power pellet (green, 50 points)
```

**Board Layout:**
The board is a 22x23 grid with:
- Walls forming the outer border
- Internal maze structure
- 4 power pellets at corners
- Ghost spawn area in the center (empty cells)
- Dots filling walkable paths

**Key Methods:**

| Method | Description |
|--------|-------------|
| `getCell(x, y)` | Get cell value at position |
| `setCell(x, y, value)` | Set cell value at position |
| `isValidPosition(x, y)` | Check if position is walkable |
| `wrapX(x)` / `wrapY(y)` | Handle cyclic wrapping |
| `getSharedVulnerableTime()` | Get remaining vulnerability time |
| `setSharedVulnerableTime(moves)` | Set vulnerability (in moves × dt) |
| `decreaseSharedVulnerableTime()` | Decrement vulnerability timer |

**Shared Vulnerability System:**
All ghosts share a single vulnerability timer. When Pacman eats a power pellet:
1. `setSharedVulnerableTime(100)` is called
2. Timer is set to `100 × dt` milliseconds
3. Each move decreases timer by `dt`
4. All ghosts check this shared timer via their `GameState` reference

---

### GhostImpl.java

**Purpose:** Represents a single ghost entity.

**Fields:**
```java
int x, y               // Current position
GameState gameState    // Reference to shared game state
```

**Key Methods:**

| Method | Description |
|--------|-------------|
| `getPos(int code)` | Returns position as "x,y" string |
| `remainTimeAsEatable(int code)` | Returns shared vulnerability time |
| `getX()` / `getY()` | Get current coordinates |
| `setPosition(x, y)` | Update ghost position |
| `setVulnerableTime(moves)` | Set shared vulnerability timer |
| `isVulnerable()` | Check if ghost is currently vulnerable |

**Note:** The `code` parameter in interface methods is unused but required for interface compatibility.

---

### GameMap.java

**Purpose:** Provides BFS pathfinding for ghost AI.

**Why Separate from Main Map Class?**
The server has its own pathfinding implementation to:
1. Keep server and client code independent
2. Avoid reflection calls to the main `Map` class
3. Simplify server-side pathfinding needs

**Key Methods:**

| Method | Description |
|--------|-------------|
| `shortestPath(p1, p2, obsColor)` | Find shortest path between two points |
| `getNeighbors(p)` | Get valid adjacent cells |
| `getPixel(x, y)` | Get cell value at position |

**Pathfinding Algorithm:**
```
BFS from start to end:
1. Initialize visited[][] and parent[][] arrays
2. Add start to queue, mark visited
3. While queue not empty:
   a. Dequeue current position
   b. If current == end, reconstruct path from parents
   c. For each neighbor:
      - If not visited and not obstacle:
        - Mark visited, set parent
        - Add to queue
4. Return path array or null if no path exists
```

**Cyclic Mode Support:**
When `_cyclicFlag` is true:
- Moving left from x=0 wraps to x=width-1
- Moving right from x=width-1 wraps to x=0
- Same for vertical edges

---

### AudioManager.java

**Purpose:** Manages all game sound effects.

**Sound Events:**

| Event | Method | Sound Description |
|-------|--------|-------------------|
| Dot eaten | `playDotEaten()` | Quick "waka" chirp (60ms) |
| Power pellet | `playPowerPellet()` | Rising energizing sound (400ms) |
| Ghost eaten | `playGhostEaten()` | Triumphant ascending sweep (300ms) |
| Game over | `playGameOver()` | Sad descending tone (1000ms) |
| Victory | `playWin()` | C-E-G fanfare (1500ms) |

**Sound Generation:**
If WAV files don't exist in `sounds/` directory, sounds are generated programmatically using sine wave synthesis:

```java
// Example: Dot eaten sound
for (int i = 0; i < buffer.length; i++) {
    double progress = (double)i / buffer.length;
    double freq = 200 + progress * 400;  // Rising frequency
    double envelope = Math.sin(Math.PI * progress);  // Smooth envelope
    double sample = Math.sin(2 * Math.PI * freq * time) * envelope;
    buffer[i] = (byte)(sample * 127);
}
```

**Custom Sound Files:**
Place WAV files in `sounds/` directory:
- `sounds/dot_eaten.wav`
- `sounds/power_pellet.wav`
- `sounds/ghost_eaten.wav`
- `sounds/game_over.wav`
- `sounds/win.wav`

---

### PacmanGame.java (Interface)

**Purpose:** Defines the contract between server and client AI.

**Direction Constants:**
```java
UP = 0
LEFT = 1
DOWN = 2
RIGHT = 3
ERR = -1
```

**Status Constants:**
```java
NOT_STARTED = 0
RUNNING = 1
PAUSED = 2
DONE = 3
WIN = 4
LOSE = 5
```

**Interface Methods:**
```java
int[][] getGame(int code)      // Get board
String getPos(int code)        // Get Pacman position
GhostCL[] getGhosts(int code)  // Get ghosts
int getStatus()                // Get game status
void move(int dir)             // Move Pacman
void play()                    // Start game
void end(int code)             // End game
```

---

### GhostCL.java (Interface)

**Purpose:** Defines the ghost interface exposed to client AI.

**Methods:**
```java
String getPos(int code)              // Get position as "x,y"
double remainTimeAsEatable(int code) // Get vulnerability time remaining
```

---

### Pixel2D.java & Index2D.java

**Purpose:** 2D coordinate representation for pathfinding.

**Pixel2D Interface:**
```java
int getX()
int getY()
double distance2D(Pixel2D p2)
```

**Index2D Implementation:**
Simple (x, y) coordinate pair with equals() based on distance.

---

## Game Flow

### Initialization Phase

```
1. Game() constructor
   └── Create AudioManager

2. init(scenario, id, cyclic, seed, resolution, dt, unused)
   ├── Create GameState(cyclic, dt)
   │     └── Initialize 22x23 board with maze
   ├── Create Random(seed)
   ├── Store scenario (0-4) and dt
   ├── initializeGame()
   │     ├── Set Pacman at (14, 11)
   │     └── Create 4 ghosts at (11, 11)
   ├── initializeGUI()
   │     ├── Canvas: 720x720 pixels
   │     ├── Coordinate system with margins
   │     └── Enable double buffering
   ├── Set status = PAUSED
   ├── waitForSpaceKey()
   │     └── Render and wait for SPACE
   └── play() → status = RUNNING
```

### Game Loop (per move)

```
move(direction)
├── If DONE → return
├── Calculate new position based on direction
├── Handle cyclic wrapping
├── If new position is valid and not WALL:
│     ├── Update Pacman position
│     ├── If cell is DOT:
│     │     ├── Clear cell
│     │     ├── Add 10 points
│     │     └── Play dot sound
│     └── If cell is POWER_PELLET:
│           ├── Clear cell
│           ├── Add 50 points
│           ├── Play power pellet sound
│           └── Set all ghosts vulnerable for 100 moves
├── checkGhostCollisions()
│     └── For each ghost at Pacman's position:
│           ├── If vulnerable: respawn ghost, +200 points, play sound
│           └── If not vulnerable: GAME OVER, play sound
├── moveCount++
├── updateGame()
│     ├── Decrease shared vulnerability timer
│     ├── moveGhosts()
│     └── Check win condition (all dots eaten)
├── checkGhostCollisions() (again, after ghost movement)
├── render()
└── StdDraw.pause(dt)
```

---

## Ghost AI System

### Movement Timing

Ghosts don't start moving until after `GHOST_START_DELAY` (50) moves. This gives the player time to prepare.

### Intelligence Levels

Ghost intelligence is controlled by the `scenario` parameter (0-4):

```
Smart Move Probability = 5% + (scenario × 10%)

Level 0: 5%  smart moves
Level 1: 15% smart moves
Level 2: 25% smart moves
Level 3: 35% smart moves
Level 4: 45% smart moves
```

### Movement Algorithm

```
For each ghost:
    If random() < smartProbability:
        // SMART MOVE: Use BFS to chase Pacman
        1. Create GameMap from current board
        2. Transpose coordinates for pathfinding
        3. Find shortest path to Pacman
        4. Move to first step on path
    Else if random() < GHOST_MOVE_PROBABILITY (30%):
        // RANDOM MOVE
        1. Pick random direction (UP/DOWN/LEFT/RIGHT)
        2. Calculate new position with cyclic wrapping
        3. If valid and not WALL, move there
    Else:
        // STAY: Ghost doesn't move this turn
```

### Ghost Respawn

When a vulnerable ghost is eaten:
1. Ghost is moved to spawn position (11, 11)
2. Score increases by 200
3. Ghost eaten sound plays
4. Ghost remains part of the game (vulnerability timer continues)

---

## Rendering System

### Coordinate System

The game uses a transposed coordinate system for display:
- Board: `board[x][y]` where x is row, y is column
- Display: `(y, x)` where y becomes screen X, x becomes screen Y

This transposition is handled in the `render()` method:
```java
double screenX = y + MARGIN;  // Board y → Screen X
double screenY = x + MARGIN;  // Board x → Screen Y
```

### Visual Elements

**Walls:**
- 3D effect with multiple layers
- Midnight blue base
- Royal blue highlight
- Neon cornflower blue edge

**Dots:**
- Soft pink glow outer layer
- Peach middle layer
- White center sparkle

**Power Pellets:**
- Multi-layer glow effect
- Green gradient from outer to inner
- Bright white center

**Pacman:**
- Loaded from `p1.png`
- Rotated based on movement direction:
  - RIGHT → 90° rotation
  - LEFT → -90° rotation
  - UP → 0° rotation
  - DOWN → 180° rotation
- Falls back to yellow circle if image missing

**Ghosts:**
- Loaded from `g0.png` to `g3.png`
- Normal size when dangerous
- Smaller with blue tint when vulnerable
- Falls back to colored circles (Red, Pink, Cyan, Orange)

### UI Elements

**Score Display:**
- Dark panel background
- Blue border
- Gold text: "★ SCORE: X ★"

**Win/Lose Messages:**
- Centered above the game board
- Green glow for victory: "🎉 PACMAN WINS! 🎉"
- Red glow for game over: "💀 GAME OVER 💀"

---

## Audio System

### Sound Generation Details

Each sound is generated using sine wave synthesis:

**Dot Eaten (60ms):**
```
Frequency: 200Hz → 600Hz (rising)
Envelope: Sine curve (smooth)
Volume: 35%
```

**Power Pellet (400ms):**
```
Base frequency: 150Hz → 650Hz (rising)
Harmonics: 1.5x and 2x base frequency
Pulsating effect: 20Hz modulation
Envelope: Attack 10%, Sustain 70%, Release 20%
```

**Ghost Eaten (300ms):**
```
Frequency: 400Hz → 1200Hz (ascending sweep)
Harmonics: 1.5x base frequency
Envelope: Fade out
```

**Game Over (1000ms):**
```
Frequency: 400Hz → 200Hz (descending)
Envelope: Linear fade out
```

**Victory (1500ms):**
```
Three-note fanfare:
- 0-33%: C5 (523Hz)
- 33-66%: E5 (659Hz)  
- 66-100%: G5 (784Hz)
Harmonics: 2x base frequency
```

---

## Configuration

### Game Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `scenario` | 0-4 | Ghost intelligence level |
| `cyclic` | true/false | Map edge wrapping |
| `seed` | long | Random seed for reproducibility |
| `dt` | int | Delay between moves (ms) |

### Timing Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `GHOST_START_DELAY` | 50 | Moves before ghosts activate |
| `GHOST_MOVE_PROBABILITY` | 0.3 | Random move chance |
| Vulnerability duration | 100 moves | Power pellet effect |

### Positions

| Entity | Initial Position |
|--------|------------------|
| Pacman | (14, 11) |
| All Ghosts | (11, 11) |

---

## File Summary

| File | Lines | Purpose |
|------|-------|---------|
| `Game.java` | ~580 | Main game controller |
| `GameState.java` | ~175 | Game data management |
| `GameMap.java` | ~115 | Server-side pathfinding |
| `GhostImpl.java` | ~47 | Ghost entity |
| `AudioManager.java` | ~265 | Sound effects |
| `PacmanGame.java` | ~56 | Game interface |
| `GhostCL.java` | ~20 | Ghost interface |
| `Pixel2D.java` | ~10 | Coordinate interface |
| `Index2D.java` | ~35 | Coordinate implementation |
| `StdDraw.java` | ~3000 | Graphics library |

---

## Dependencies

- **StdDraw:** Princeton's standard drawing library for graphics
- **javax.sound.sampled:** Java's built-in audio API for sound generation

---

## Testing

Server classes have JUnit tests in `test/`:
- `GameStateTest.java` - Tests for GameState
- `GhostImplTest.java` - Tests for GhostImpl
- `GameTest.java` - Tests for Game

Run tests:
```bash
javac -cp "src;libs/*" -d tmp_compile src/*.java src/server/*.java test/*.java
java -cp "tmp_compile;libs/*" org.junit.runner.JUnitCore GameStateTest GhostImplTest GameTest
```
