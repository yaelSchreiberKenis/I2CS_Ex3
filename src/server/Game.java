package server;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Main game server implementation
 * Manages game state, logic, and provides interface for algorithms
 */
public class Game implements PacmanGame {
    
    // Direction constants (for Ex3Algo compatibility)
    public static final int UP = 0;
    public static final int LEFT = 1;
    public static final int DOWN = 2;
    public static final int RIGHT = 3;
    public static final int ERR = -1;
    
    /**
     * Static utility method to convert Color to int (for Ex3Algo)
     */
    public static int getIntColor(Color color, int code) {
        if (color.equals(Color.BLUE)) return GameState.WALL;
        if (color.equals(Color.PINK)) return GameState.DOT;
        if (color.equals(Color.GREEN)) return GameState.POWER_PELLET;
        return GameState.EMPTY;
    }
    private GameState gameState;
    private Random random;
    private int lastPacmanDirection = RIGHT; // Track last direction for Pacman rotation
    private int dt; // Delay time between moves in milliseconds
    private int moveCount = 0; // Track number of moves (for delaying ghost movement)
    private int scenario = 0; // Scenario (0-4) for smart ghost movement probability
    private static final int DOT_SCORE = 10;
    private static final int POWER_PELLET_SCORE = 50;
    private static final int GHOST_SCORE = 200;
    private static final int GHOST_START_DELAY = 50; // Ghosts don't move for first 50 moves
    private static final double GHOST_MOVE_PROBABILITY = 0.3; // Ghosts move 30% of the time (slower)
    private static final int GHOST_START_X = 11; // Ghost starting X position
    private static final int GHOST_START_Y = 11; // Ghost starting Y position

    public Game() {}

    public void init(int scenario, String id, boolean cyclic, long seed, 
                     double resolution, int dt, int unused) {
        this.gameState = new GameState(cyclic, dt);
        this.random = new Random(seed);
        this.dt = dt;
        this.moveCount = 0; // Reset move counter
        this.scenario = Math.max(0, Math.min(4, scenario)); // Store scenario (0-4)
        initializeGame();
        initializeGUI();
        // Start in PAUSED state - wait for space key to start
        gameState.setStatus(PacmanGame.PAUSED);
        waitForSpaceKey();
        play();
    }
    
    /**
     * Wait for space key press before starting the game
     */
    private void waitForSpaceKey() {
        // Render initial state
        render();
        StdDraw.show();
        
        // Wait for space key
        while (true) {
            if (StdDraw.hasNextKeyTyped()) {
                char key = StdDraw.nextKeyTyped();
                if (key == ' ') {
                    break; // Space pressed, start the game
                }
            }
            StdDraw.pause(50); // Small delay to avoid busy waiting
        }
    }
    
    private void initializeGame() {
        // Set initial Pacman position
        gameState.setPacmanPosition(14, 11);
        
        // Always add exactly 4 ghosts
        for (int i = 0; i < 4; i++) {
            addGhost(GHOST_START_X, GHOST_START_Y);
        }
    }
    
    private void addGhost(int x, int y) {
        if (gameState == null) return;
        GhostImpl ghost = new GhostImpl(x, y, gameState);
        gameState.addGhost(ghost);
    }
    
    @Override
    public int[][] getGame(int code) {
        if (gameState == null) return new int[0][0];
        return gameState.getBoard();
    }
    
    @Override
    public String getPos(int code) {
        if (code == 0) { // Pacman
            return gameState.getPacmanX() + "," + gameState.getPacmanY();
        }
        return "0,0";
    }
    
    @Override
    public GhostCL[] getGhosts(int code) {
        if (gameState == null) return new GhostCL[0];
        List<GhostImpl> ghosts = gameState.getGhosts();
        return ghosts.toArray(new GhostCL[ghosts.size()]);
    }
    
    
    @Override
    public int getStatus() {
        if (gameState == null) return PacmanGame.NOT_STARTED;
        return gameState.getStatus();
    }
    
    @Override
    public void move(int dir) {
        if (gameState == null) return;
        if (gameState.getStatus() != PacmanGame.RUNNING) {
            return;
        }
        
        int dx = 0, dy = 0;
        switch (dir) {
            case UP:
                dy = 1; // Up increases y (small y's are down)
                break;
            case DOWN:
                dy = -1; // Down decreases y
                break;
            case LEFT:
                dx = -1;
                break;
            case RIGHT:
                dx = 1;
                break;
            default:
                return;
        }
        
        int newX = gameState.getPacmanX() + dx;
        int newY = gameState.getPacmanY() + dy;
        
        // Handle cyclic wrapping
        newX = gameState.wrapX(newX);
        newY = gameState.wrapY(newY);
        
        // Check that the position is valid AND not a wall (prevents moving into walls)
        if (gameState.isValidPosition(newX, newY) && gameState.getCell(newX, newY) != GameState.WALL) {
            gameState.setPacmanPosition(newX, newY);
            lastPacmanDirection = dir; // Update direction
            
            // Check what's at the new position
            int cell = gameState.getCell(newX, newY);
            if (cell == GameState.DOT) {
                gameState.setCell(newX, newY, GameState.EMPTY);
                gameState.addScore(DOT_SCORE);
            } else if (cell == GameState.POWER_PELLET) {
                gameState.setCell(newX, newY, GameState.EMPTY);
                gameState.addScore(POWER_PELLET_SCORE);
                // Make all ghosts vulnerable for 100 moves
                for (GhostImpl ghost : gameState.getGhosts()) {
                    ghost.setVulnerableTime(100);
                }
            }
        }
        
        // Always check collision after Pacman's move (even if Pacman didn't move)
        checkGhostCollisions();
        
        moveCount++; // Increment move counter
        updateGame();
        
        // Check collision again after ghosts move (ghosts might have moved into Pacman)
        checkGhostCollisions();
        render();
        // Slow the game using dt
        StdDraw.pause(dt);
    }
    
    private void checkGhostCollisions() {
        int pacX = gameState.getPacmanX();
        int pacY = gameState.getPacmanY();
        
        for (GhostImpl ghost : gameState.getGhosts()) {
            if (ghost.getX() == pacX && ghost.getY() == pacY) {
                if (ghost.isVulnerable()) {
                    // Eat ghost - move it back to starting position
                    ghost.setPosition(GHOST_START_X, GHOST_START_Y);
                    gameState.addScore(GHOST_SCORE);
                } else {
                    // Collision with non-vulnerable ghost - game over
                    gameState.setStatus(PacmanGame.DONE);
                }
            }
        }
    }
    
    private void updateGame() {
        // Update shared ghost vulnerable time (synchronized for all ghosts) - move-based (1.0 per move)
        gameState.decreaseSharedVulnerableTime(); // Decrement by 1 move per update
        
        // Simple ghost AI - move randomly
        moveGhosts();
        
        // Check win condition
        if (allDotsEaten()) {
            gameState.setStatus(PacmanGame.DONE);
        }
    }
    
    private void moveGhosts() {
        // Don't move ghosts for the first GHOST_START_DELAY moves
        if (moveCount < GHOST_START_DELAY) {
            return;
        }
        
        int pacX = gameState.getPacmanX();
        int pacY = gameState.getPacmanY();
        
        // Smart movement probability based on scenario level (0-4)
        // Level 0: 5%, Level 1: 10%, Level 2: 15%, Level 3: 20%, Level 4: 25%
        double smartProbability = 0.05 + (scenario * 0.05);
        
        List<GhostImpl> ghosts = gameState.getGhosts();
        for (int i = 0; i < ghosts.size(); i++) {
            GhostImpl ghost = ghosts.get(i);
            int ghostX = ghost.getX();
            int ghostY = ghost.getY();
            
            // All ghosts can be smart, but with probability based on level
            if (random.nextDouble() < smartProbability) {
                // Smart move: use BFS to find direction towards Pacman
                int[] nextMove = findNextStepTowardsPacman(ghostX, ghostY, pacX, pacY);
                if (nextMove != null) {
                    int newX = nextMove[0];
                    int newY = nextMove[1];
                    if (gameState.isValidPosition(newX, newY) && 
                        gameState.getCell(newX, newY) != GameState.WALL) {
                        ghost.setPosition(newX, newY);
                        continue;
                    }
                }
            }
            
            // Random move (when not being smart or smart move failed)
            if (random.nextDouble() < GHOST_MOVE_PROBABILITY) {
                int dir = random.nextInt(4);
                int dx = 0, dy = 0;
                switch (dir) {
                    case 0: dy = 1; break;  // UP
                    case 1: dy = -1; break; // DOWN
                    case 2: dx = -1; break; // LEFT
                    case 3: dx = 1; break;  // RIGHT
                }
                
                int newX = gameState.wrapX(ghostX + dx);
                int newY = gameState.wrapY(ghostY + dy);
                
                if (gameState.isValidPosition(newX, newY) && 
                    gameState.getCell(newX, newY) != GameState.WALL) {
                    ghost.setPosition(newX, newY);
                }
            }
        }
    }
    
    /**
     * Simple BFS to find next step towards Pacman (game-specific pathfinding).
     * Returns [newX, newY] or null if no path found.
     */
    private int[] findNextStepTowardsPacman(int fromX, int fromY, int toX, int toY) {
        if (fromX == toX && fromY == toY) return null;
        
        int width = gameState.getWidth();
        int height = gameState.getHeight();
        boolean[][] visited = new boolean[width][height];
        int[][] parentX = new int[width][height];
        int[][] parentY = new int[width][height];
        
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{fromX, fromY});
        visited[fromX][fromY] = true;
        parentX[fromX][fromY] = -1;
        parentY[fromX][fromY] = -1;
        
        int[][] dirs = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};
        
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int cx = curr[0], cy = curr[1];
            
            if (cx == toX && cy == toY) {
                // Backtrack to find first step
                int pathX = cx, pathY = cy;
                while (parentX[pathX][pathY] != fromX || parentY[pathX][pathY] != fromY) {
                    int px = parentX[pathX][pathY];
                    int py = parentY[pathX][pathY];
                    if (px == -1) break;
                    pathX = px;
                    pathY = py;
                }
                return new int[]{pathX, pathY};
            }
            
            for (int[] d : dirs) {
                int nx = gameState.wrapX(cx + d[0]);
                int ny = gameState.wrapY(cy + d[1]);
                
                // Check bounds BEFORE accessing arrays to prevent overflow in non-cyclic mode
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                
                if (!visited[nx][ny] && gameState.isValidPosition(nx, ny) && 
                    gameState.getCell(nx, ny) != GameState.WALL) {
                    visited[nx][ny] = true;
                    parentX[nx][ny] = cx;
                    parentY[nx][ny] = cy;
                    queue.offer(new int[]{nx, ny});
                }
            }
        }
        return null;
    }
    
    
    private boolean allDotsEaten() {
        for (int x = 0; x < gameState.getWidth(); x++) {
            for (int y = 0; y < gameState.getHeight(); y++) {
                int cell = gameState.getCell(x, y);
                if (cell == GameState.DOT) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void play() {
        if (gameState == null) return;
        if (gameState.getStatus() == PacmanGame.NOT_STARTED || gameState.getStatus() == PacmanGame.PAUSED) {
            gameState.setStatus(PacmanGame.RUNNING);
        } else if (gameState.getStatus() == PacmanGame.RUNNING) {
            gameState.setStatus(PacmanGame.PAUSED);
        }
    }
    
    @Override
    public void end(int code) {
        if (gameState == null) return;
        gameState.setStatus(PacmanGame.DONE);
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    // GUI and rendering
    private static final double TOP_SPACE = 3.0; // Space at top for score/messages
    private static final double MARGIN = 1.5; // Margin around the map
    private static final double CELL_SIZE = 0.95; // Size of Pacman and normal ghosts (nearly full cell)
    private static final double VULNERABLE_GHOST_SIZE = 0.55; // Smaller size for vulnerable ghosts
    
    private void initializeGUI() {
        if (gameState == null) return;
        int width = gameState.getWidth();
        int height = gameState.getHeight();
        
        // Set canvas size
        StdDraw.setCanvasSize(800, 800);
        
        // Set coordinate system with margins around the map
        // Map area: transposed, so width becomes height and height becomes width
        int displayWidth = height; // Transposed: original height becomes display width
        int displayHeight = width; // Transposed: original width becomes display height
        
        // Adjusted: add more bottom margin to push game up in the UI
        double bottomMargin = MARGIN + 2.5;
        StdDraw.setXscale(-MARGIN, displayWidth + 2*MARGIN);
        StdDraw.setYscale(-bottomMargin, displayHeight + TOP_SPACE + MARGIN);
        
        // Enable double buffering for smooth animation
        StdDraw.enableDoubleBuffering();
        
        // Set background to black
        StdDraw.clear(Color.BLACK);
    }
    
    private void render() {
        if (gameState == null) return;
        
        // Clear screen with black background
        StdDraw.clear(Color.BLACK);
        
        int width = gameState.getWidth();
        int height = gameState.getHeight();
        
        // Draw board transposed (swap x and y for display)
        // board[x][y] is displayed at position (y, x)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int cell = gameState.getCell(x, y);
                
                // Transpose: display board[x][y] at screen position (y, x)
                // Space at top: board is at bottom, space is at top (above the board)
                double screenX = y + MARGIN; // Transposed: original y becomes screen x
                double screenY = x + MARGIN; // Transposed: original x becomes screen y (board at bottom)
                
                // Draw cell content
                if (cell == GameState.WALL) {
                    // Draw blue filled square with darker border for depth
                    StdDraw.setPenColor(new Color(0, 0, 139)); // Dark blue fill
                    StdDraw.filledSquare(screenX + 0.5, screenY + 0.5, 0.45);
                    StdDraw.setPenColor(new Color(30, 144, 255)); // Lighter blue border
                    StdDraw.setPenRadius(0.003);
                    StdDraw.square(screenX + 0.5, screenY + 0.5, 0.45);
                    StdDraw.setPenRadius();
                } else if (cell == GameState.DOT) {
                    // Draw small glowing dot
                    StdDraw.setPenColor(new Color(255, 200, 200)); // Light pink glow
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.12);
                    StdDraw.setPenColor(Color.WHITE);
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.08);
                } else if (cell == GameState.POWER_PELLET) {
                    // Draw pulsing power pellet (larger, with glow effect)
                    StdDraw.setPenColor(new Color(144, 238, 144)); // Light green glow
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.38);
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.30);
                    StdDraw.setPenColor(new Color(200, 255, 200)); // Bright center
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.15);
                }
                // EMPTY cells: just black background
            }
        }
        
        // Draw Pacman with rotation based on direction (transposed coordinates)
        int pacX = gameState.getPacmanX();
        int pacY = gameState.getPacmanY();
        // Transpose: display at (pacY, pacX)
        double pacScreenX = pacY + MARGIN;
        double pacScreenY = pacX + MARGIN; // Board at bottom, space at top
        double pacRotation = 0.0;
        boolean flipHorizontal = false;
        
        // Calculate rotation/flip based on direction (transposed display)
        // After transposition: RIGHT moves up, LEFT moves down, UP moves right, DOWN moves left
        // Image defaults to facing right
        switch (lastPacmanDirection) {
            case RIGHT:
                pacRotation = 90.0; // RIGHT (dx=1) moves up on screen → rotate 90° to face up
                break;
            case LEFT:
                pacRotation = -90.0; // LEFT (dx=-1) moves down on screen → rotate -90° to face down
                break;
            case UP:
                pacRotation = 0.0; // UP (dy=1) moves right on screen → no rotation (already faces right)
                break;
            case DOWN:
                flipHorizontal = true; // DOWN (dy=-1) moves left on screen → rotate 180° to face left
                pacRotation = 180.0;
                break;
        }
        
        try {
            // Draw Pacman at full cell size with appropriate rotation
            if (flipHorizontal) {
                StdDraw.picture(pacScreenX + 0.5, pacScreenY + 0.5, "p1.png", CELL_SIZE, CELL_SIZE, 180.0);
            } else {
                StdDraw.picture(pacScreenX + 0.5, pacScreenY + 0.5, "p1.png", CELL_SIZE, CELL_SIZE, pacRotation);
            }
        } catch (Exception e) {
            // Fallback: draw Pacman as yellow circle with mouth
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.filledCircle(pacScreenX + 0.5, pacScreenY + 0.5, CELL_SIZE * 0.45);
        }
        
        // Draw ghosts
        List<GhostImpl> ghosts = gameState.getGhosts();
        String[] ghostImages = {"g0.png", "g1.png", "g2.png", "g3.png"};
        Color[] ghostColors = {Color.RED, Color.PINK, Color.CYAN, Color.ORANGE}; // Classic ghost colors
        
        for (int i = 0; i < ghosts.size() && i < 4; i++) {
            GhostImpl ghost = ghosts.get(i);
            int gX = ghost.getX();
            int gY = ghost.getY();
            // Skip if ghost is off screen (eaten)
            if (gX < 0 || gY < 0) continue;
            
            // Transpose ghost coordinates: display at (gY, gX)
            double ghostScreenX = gY + MARGIN;
            double ghostScreenY = gX + MARGIN;
            
            // Ghost size: full cell when normal, smaller when vulnerable
            double ghostSize = ghost.isVulnerable() ? VULNERABLE_GHOST_SIZE : CELL_SIZE;
            
            try {
                if (ghost.isVulnerable()) {
                    // Draw vulnerable ghost as blue with smaller size
                    StdDraw.picture(ghostScreenX + 0.5, ghostScreenY + 0.5, ghostImages[i], ghostSize, ghostSize);
                    // Add blue tint overlay
                    StdDraw.setPenColor(new Color(0, 0, 255, 100)); // Semi-transparent blue
                    StdDraw.filledCircle(ghostScreenX + 0.5, ghostScreenY + 0.5, ghostSize * 0.4);
                } else {
                    // Normal ghost at full cell size
                    StdDraw.picture(ghostScreenX + 0.5, ghostScreenY + 0.5, ghostImages[i], ghostSize, ghostSize);
                }
            } catch (Exception e) {
                // Fallback: draw ghost as colored circle
                if (ghost.isVulnerable()) {
                    StdDraw.setPenColor(Color.BLUE);
                    StdDraw.filledCircle(ghostScreenX + 0.5, ghostScreenY + 0.5, VULNERABLE_GHOST_SIZE * 0.4);
                } else {
                    StdDraw.setPenColor(ghostColors[i]);
                    StdDraw.filledCircle(ghostScreenX + 0.5, ghostScreenY + 0.5, CELL_SIZE * 0.45);
                }
            }
        }
        
        // Draw score at top
        int displayWidth = height;
        int displayHeight = width;
        double centerX = displayWidth / 2.0 + MARGIN;
        double topY = displayHeight + MARGIN + TOP_SPACE * 0.7;
        
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
        StdDraw.text(centerX, topY, "Score: " + gameState.getScore());
        
        // Draw win/lose message above the table if game is done
        if (gameState.getStatus() == PacmanGame.DONE) {
            double msgY = displayHeight + MARGIN + TOP_SPACE * 0.3;
            
            StdDraw.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 28));
            
            if (allDotsEaten()) {
                StdDraw.setPenColor(Color.GREEN);
                StdDraw.text(centerX, msgY, "PACMAN WINS! :)");
            } else {
                StdDraw.setPenColor(Color.RED);
                StdDraw.text(centerX, msgY, "GAME OVER :(");
            }
        }
        
        // Show the frame
        StdDraw.show();
    }
}
