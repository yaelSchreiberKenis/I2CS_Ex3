# Running Guide for Pacman Game Server

This guide explains how to run the server and client components from IntelliJ IDEA.

## Project Structure

- **Server package** (`src/server/`): Contains game logic and server implementation
  - `PacmanGameServer`: Main game server
  - `GameState`: Game state management
  - `GhostImpl`: Ghost entity implementation
  - `ServerRunner`: Standalone server runner (for testing without GUI)

- **Client package** (`src/client/`): Contains GUI client
  - `GameClient`: GUI client using StdDraw
  - `GameMain`: Main entry point (runs server + GUI together)

## Running the Game

### Option 1: Run Server + GUI Together (Recommended)

This runs the integrated game with GUI:

1. **In IntelliJ IDEA:**
   - Right-click on `src/client/GameMain.java`
   - Select **Run 'GameMain.main()'**

2. **Or create a Run Configuration:**
   - Go to **Run** → **Edit Configurations**
   - Click **+** → **Application**
   - Name: `GameMain`
   - Main class: `client.GameMain`
   - Module: Your module name
   - Click **OK**
   - Run it

### Option 2: Run Server Only (Testing)

For testing the server without GUI:

1. **In IntelliJ IDEA:**
   - Right-click on `src/server/ServerRunner.java`
   - Select **Run 'ServerRunner.main()'**

2. **Or create a Run Configuration:**
   - Go to **Run** → **Edit Configurations**
   - Click **+** → **Application**
   - Name: `ServerRunner`
   - Main class: `server.ServerRunner`
   - Module: Your module name
   - Click **OK**
   - Run it

## Running from Command Line

### Server Only:
```bash
cd "C:\Users\ilank\Documents\I2CS_Ex3"
javac -cp "libs/*;out/production/I2CS_Ex3" -d out/production/I2CS_Ex3 src/server/*.java
java -cp "libs/*;out/production/I2CS_Ex3" server.ServerRunner
```

### Server + GUI:
```bash
cd "C:\Users\ilank\Documents\I2CS_Ex3"
javac -cp "libs/*;out/production/I2CS_Ex3" -d out/production/I2CS_Ex3 src/server/*.java src/client/*.java
java -cp "libs/*;out/production/I2CS_Ex3" client.GameMain
```

## Game Controls

- **Space**: Start/Pause game
- **Q**: Quit game
- The game runs automatically (AI moves Pacman)

## Notes

- The game uses `StdDraw` from the JAR file for rendering
- Server and client run in the same process (not networked)
- The server manages game logic, client handles display
