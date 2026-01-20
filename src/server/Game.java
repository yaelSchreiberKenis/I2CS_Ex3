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
                // Make all ghosts vulnerable for 50 moves
                for (GhostImpl ghost : gameState.getGhosts()) {
                    ghost.setVulnerableTime(50.0); // 50 moves
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
        
        // Create Map for pathfinding (only once per move)
        int[][] board = gameState.getBoard();
        Object map = createMapFromBoard(board);
        
        // Move probability for smart ghost = scenario * 25% (0-100%)
        double smartGhostProbability = scenario * 0.15;
        
        List<GhostImpl> ghosts = gameState.getGhosts();
        for (int i = 0; i < ghosts.size(); i++) {
            GhostImpl ghost = ghosts.get(i);
            int ghostX = ghost.getX();
            int ghostY = ghost.getY();
            
            boolean shouldMove = false;
            int newX = ghostX;
            int newY = ghostY;
            
            if (i == 0) {
                // First ghost (index 0) is smart - chase Pacman using shortest path
                if (map != null && random.nextDouble() < smartGhostProbability) {
                    try {
                        // Calculate shortest path using Map via reflection
                        Class<?> index2DClass = Class.forName("Index2D");
                        Object ghostPos = index2DClass.getConstructor(int.class, int.class).newInstance(ghostX, ghostY);
                        Object pacmanPos = index2DClass.getConstructor(int.class, int.class).newInstance(pacX, pacY);
                        
                        Class<?> pixel2DClass = Class.forName("Pixel2D");
                        java.lang.reflect.Method shortestPathMethod = map.getClass().getMethod("shortestPath", 
                            pixel2DClass, pixel2DClass, int.class);
                        Object[] path = (Object[])shortestPathMethod.invoke(map, ghostPos, pacmanPos, GameState.WALL);
                        
                        if (path != null && path.length > 1) {
                            // Get next step from path
                            Object nextStep = path[1];
                            java.lang.reflect.Method getXMethod = pixel2DClass.getMethod("getX");
                            java.lang.reflect.Method getYMethod = pixel2DClass.getMethod("getY");
                            newX = (Integer)getXMethod.invoke(nextStep);
                            newY = (Integer)getYMethod.invoke(nextStep);
                            shouldMove = true;
                        }
                    } catch (Exception e) {
                        // If pathfinding fails, don't move
                    }
                }
            } else {
                // Other ghosts (indices 1,2,3) move randomly
                if (random.nextDouble() < GHOST_MOVE_PROBABILITY) {
                    int dir = random.nextInt(4);
                    int dx = 0, dy = 0;
                    switch (dir) {
                        case 0: dy = 1; break;  // UP (increases y)
                        case 1: dy = -1; break; // DOWN (decreases y)
                        case 2: dx = -1; break; // LEFT
                        case 3: dx = 1; break;  // RIGHT
                    }
                    
                    newX = gameState.wrapX(ghostX + dx);
                    newY = gameState.wrapY(ghostY + dy);
                    shouldMove = true;
                }
            }
            
            // Apply move if valid
            if (shouldMove && gameState.isValidPosition(newX, newY) && 
                gameState.getCell(newX, newY) != GameState.WALL) {
                ghost.setPosition(newX, newY);
            }
        }
    }
    
    /**
     * Creates a Map instance from the game board, similar to Ex3Algo.
     * Uses reflection to access default package classes.
     */
    private Object createMapFromBoard(int[][] board) {
        try {
            // Transpose board: board[x][y] -> transposedBoard[y][x]
            // Map stores as _map[y][x] and getPixel(x,y) returns _map[y][x]
            int width = board.length;
            int height = board[0].length;
            int[][] transposedBoard = new int[height][width];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    transposedBoard[y][x] = board[x][y];
                }
            }
            
            // Use reflection to create Map instance (default package)
            Class<?> mapClass = Class.forName("Map");
            java.lang.reflect.Constructor<?> mapConstructor = mapClass.getConstructor(int[][].class);
            Object mapObj = mapConstructor.newInstance((Object)transposedBoard);
            
            // Set cyclic mode
            java.lang.reflect.Method setCyclicMethod = mapClass.getMethod("setCyclic", boolean.class);
            setCyclicMethod.invoke(mapObj, gameState.isCyclicMode());
            
            return mapObj;
        } catch (Exception e) {
            // If Map class not found, return null (fall back to random movement)
            return null;
        }
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
    private static final double TOP_SPACE = 3.0; // Space at top for future use
    private static final double MARGIN = 2.0; // Margin around the map
    private static final double IMAGE_SIZE = 0.6; // Size of Pacman and ghost images (much smaller to fit in cell)
    
    private void initializeGUI() {
        if (gameState == null) return;
        int width = gameState.getWidth();
        int height = gameState.getHeight();
        
        // Set canvas size
        StdDraw.setCanvasSize(800, 800);
        
        // Set coordinate system with margins around the map
        // Map area: transposed, so width becomes height and height becomes width
        // Space at top: board is at bottom, space is at top
        int displayWidth = height; // Transposed: original height becomes display width
        int displayHeight = width; // Transposed: original width becomes display height
        StdDraw.setXscale(-MARGIN, displayWidth + 2*MARGIN);
        StdDraw.setYscale(-MARGIN, displayHeight + TOP_SPACE + 2*MARGIN);
        
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
                    // Draw blue square outline (empty in middle)
                    StdDraw.setPenColor(Color.BLUE);
                    double wall_size = 0.004;
                    StdDraw.setPenRadius(wall_size);
                    StdDraw.square(screenX + 0.5, screenY + 0.5, 0.5 - wall_size - 0.1);
                    StdDraw.setPenRadius(); // Reset to default
                } else if (cell == GameState.DOT) {
                    // Draw small pink circle in middle of cell
                    StdDraw.setPenColor(Color.PINK);
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.15);
                } else if (cell == GameState.POWER_PELLET) {
                    // Draw green big circle in middle of cell
                    StdDraw.setPenColor(Color.GREEN);
                    StdDraw.filledCircle(screenX + 0.5, screenY + 0.5, 0.35);
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
            // For left direction, we need to mirror. Since StdDraw doesn't support mirroring directly,
            // we'll use rotation 180 degrees which gives a similar effect (though not perfect mirror)
            if (flipHorizontal) {
                // Use 180 degree rotation for mirroring effect
                StdDraw.picture(pacScreenX + 0.5, pacScreenY + 0.5, "p1.png", IMAGE_SIZE, IMAGE_SIZE, 180.0);
            } else {
                // Use scaled and rotated image
                StdDraw.picture(pacScreenX + 0.5, pacScreenY + 0.5, "p1.png", IMAGE_SIZE, IMAGE_SIZE, pacRotation);
            }
        } catch (Exception e) {
            // If image not found, draw yellow circle as fallback
            StdDraw.setPenColor(Color.YELLOW);
            StdDraw.filledCircle(pacScreenX + 0.5, pacScreenY + 0.5, IMAGE_SIZE * 0.6);
        }
        
        // Draw ghosts
        List<GhostImpl> ghosts = gameState.getGhosts();
        String[] ghostImages = {"g0.png", "g1.png", "g2.png", "g3.png"};
        double normalGhostSize = 0.75; // Bigger than IMAGE_SIZE but still fits in cell
        double vulnerableGhostSize = 0.45; // Smaller when vulnerable
        for (int i = 0; i < ghosts.size() && i < 4; i++) {
            GhostImpl ghost = ghosts.get(i);
            int gX = ghost.getX();
            int gY = ghost.getY();
            // Skip if ghost is off screen (eaten)
            if (gX < 0 || gY < 0) continue;
            
            // Transpose ghost coordinates: display at (gY, gX)
            double ghostScreenX = gY + MARGIN;
            double ghostScreenY = gX + MARGIN; // Board at bottom, space at top
            
            // Determine ghost size based on vulnerability
            double ghostSize = ghost.isVulnerable() ? vulnerableGhostSize : normalGhostSize;
            
            try {
                // Draw scaled ghost images - bigger normally, smaller when vulnerable
                StdDraw.picture(ghostScreenX + 0.5, ghostScreenY + 0.5, ghostImages[i], ghostSize, ghostSize);
            } catch (Exception e) {
                // If image not found, draw colored circle as fallback
                if (ghost.isVulnerable()) {
                    StdDraw.setPenColor(Color.BLUE); // Blue when vulnerable
                } else {
                    StdDraw.setPenColor(Color.RED);
                }
                StdDraw.filledCircle(ghostScreenX + 0.5, ghostScreenY + 0.5, ghostSize * 0.6);
            }
        }
        
        // Draw win/lose message above the table if game is done
        if (gameState.getStatus() == PacmanGame.DONE) {
            // Transposed: original height becomes display width, original width becomes display height
            int displayWidth = height; // Transposed: original height becomes display width
            int displayHeight = width; // Transposed: original width becomes display height
            double centerX = displayWidth / 2.0 + MARGIN;
            double centerY = displayHeight + MARGIN + TOP_SPACE / 2.0; // Center of top space
            
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
            
            // Check if won (all dots eaten) or lost (collision with ghost)
            if (allDotsEaten()) {
                StdDraw.text(centerX, centerY, "packman win! :)");
            } else {
                StdDraw.text(centerX, centerY, "packman lose :(");
            }
        }
        
        // Show the frame
        StdDraw.show();
    }
}
