# Running Guide for Pacman AI Game

This guide explains how to build and run the Pacman AI game.

## Project Structure

```
I2CS_Ex3/
├── src/
│   ├── client/         # Client-side: AI algorithm and entry point
│   │   ├── Ex3Main.java        # Main entry point
│   │   ├── Ex3Algo.java        # AI algorithm
│   │   ├── GameInfo.java       # Game settings
│   │   ├── ManualAlgo.java     # Manual control
│   │   ├── Map.java            # Pathfinding map
│   │   ├── Map2D.java          # Map interface
│   │   ├── Index2D.java        # 2D coordinates
│   │   └── Pixel2D.java        # Coordinate interface
│   │
│   └── server/         # Server-side: Game engine
│       ├── Game.java           # Main game logic
│       ├── GameState.java      # State management
│       ├── GhostImpl.java      # Ghost entities
│       ├── AudioManager.java   # Sound effects
│       └── StdDraw.java        # Graphics
│
├── resources/          # Game assets (images, sounds)
│   ├── p1.png          # Pacman sprite
│   └── g0-g3.png       # Ghost sprites
│
├── dist/               # Distribution folder (after build)
│   └── PacmanAI.jar    # Executable JAR
│
├── build.bat           # Windows build script
└── build.sh            # Unix/Mac build script
```

## Quick Start

### Option 1: Run Pre-built JAR (Recommended)

After building, simply run:

```bash
cd dist
java -jar PacmanAI.jar
```

### Option 2: Build and Run

#### Windows
```batch
# Build
.\build.bat

# Run
java -jar dist\PacmanAI.jar
```

#### Linux/Mac
```bash
# Make build script executable
chmod +x build.sh

# Build
./build.sh

# Run
java -jar dist/PacmanAI.jar
```

### Option 3: Run from IntelliJ IDEA

1. **Open the project** in IntelliJ IDEA
2. **Right-click** on `src/client/Ex3Main.java`
3. **Select** "Run 'Ex3Main.main()'"

Or create a Run Configuration:
- Go to **Run** → **Edit Configurations**
- Click **+** → **Application**
- Name: `PacmanAI`
- Main class: `client.Ex3Main`
- Working directory: Project root
- Click **OK** and run

## Building from Command Line (Without Scripts)

### Compile
```bash
# Create output directory
mkdir -p build/classes

# Compile all source files
javac -cp "src;libs/*" -d build/classes src/client/*.java src/server/*.java
```

### Create JAR
```bash
# Create resources in build
mkdir -p build/classes/resources
cp resources/*.png build/classes/resources/

# Create JAR
cd build/classes
jar cfm ../../dist/PacmanAI.jar ../../MANIFEST.MF *
cd ../..
```

### Run
```bash
java -jar dist/PacmanAI.jar
```

## Game Controls

| Key | Action |
|-----|--------|
| **Space** | Start/Pause game |
| **W** | Move up (manual mode) |
| **A** | Move left (manual mode) |
| **X** | Move down (manual mode) |
| **D** | Move right (manual mode) |

## Configuration

Edit `src/client/GameInfo.java` to customize:

```java
public class GameInfo {
    // Difficulty level (0-4, higher = smarter ghosts)
    public static final int CASE_SCENARIO = 4;
    
    // Enable edge wrapping
    public static final boolean CYCLIC_MODE = false;
    
    // Game speed (20-200 ms per move)
    public static final int DT = 200;
    
    // Choose algorithm: _myAlgo (AI) or _manualAlgo (keyboard)
    public static final PacManAlgo ALGO = _myAlgo;
}
```

## Running Tests

### From IntelliJ
Right-click on `test/` folder → **Run 'All Tests'**

### From Command Line
```bash
# Compile tests
javac -cp "src;libs/*" -d build/classes test/*.java

# Run tests
java -cp "build/classes;libs/*" org.junit.runner.JUnitCore MapTest Index2DTest GameTest GameStateTest GhostImplTest
```

## Troubleshooting

### "Could not find or load main class"
- Make sure you're running from the project root directory
- Check that the JAR was built successfully

### Images not loading
- Ensure `resources/` folder is in the same directory as the JAR
- Or run from the project root directory

### No sound
- Sound effects are generated programmatically
- Check system audio is not muted

## Distribution Contents

After running `build.bat` or `build.sh`, the `dist/` folder contains:

| File/Folder | Description |
|-------------|-------------|
| `PacmanAI.jar` | Executable game JAR |
| `resources/` | Images and sounds |
| `src/` | Complete source code |
| `test/` | JUnit test files |
| `libs/` | JUnit dependencies |
| `Readme.md` | Project documentation |

To distribute the game, share the entire `dist/` folder.
