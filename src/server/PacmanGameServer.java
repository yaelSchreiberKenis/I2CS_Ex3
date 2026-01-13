import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main game server implementation
 * Manages game state, logic, and provides interface for algorithms
 */
public class PacmanGameServer implements PacmanGameInterface {
    private GameState gameState;
    private Random random;
    private Character lastKeyChar;
    private long lastUpdateTime;
    private static final int DOT_SCORE = 10;
    private static final int POWER_PELLET_SCORE = 50;
    private static final int GHOST_SCORE = 200;
    
    public PacmanGameServer(int width, int height, boolean cyclic, long randomSeed) {
        this.gameState = new GameState(width, height, cyclic);
        this.random = new Random(randomSeed);
        this.lastKeyChar = null;
        this.lastUpdateTime = System.currentTimeMillis();
        initializeGame();
    }
    
    private void initializeGame() {
        // Set initial Pacman position (top-left area)
        gameState.setPacmanPosition(1, 1);
        
        // Add some ghosts (avoid initial Pacman position)
        addGhost(8, 8);
        addGhost(10, 10);
        addGhost(12, 12);
        
        // Add some power pellets (avoid ghost positions)
        gameState.setCell(6, 6, GameState.POWER_PELLET);
        gameState.setCell(11, 11, GameState.POWER_PELLET);
    }
    
    private void addGhost(int x, int y) {
        GhostImpl ghost = new GhostImpl(x, y);
        gameState.addGhost(ghost);
    }
    
    @Override
    public int[][] getGame(int code) {
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
    public GhostInterface[] getGhosts(int code) {
        List<GhostImpl> ghosts = gameState.getGhosts();
        return ghosts.toArray(new GhostInterface[ghosts.size()]);
    }
    
    @Override
    public int getStatus() {
        return gameState.getStatus();
    }
    
    @Override
    public void move(int dir) {
        if (gameState.getStatus() != RUNNING) {
            return;
        }
        
        int dx = 0, dy = 0;
        switch (dir) {
            case UP:
                dy = -1;
                break;
            case DOWN:
                dy = 1;
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
        
        if (gameState.isValidPosition(newX, newY)) {
            gameState.setPacmanPosition(newX, newY);
            
            // Check what's at the new position
            int cell = gameState.getCell(newX, newY);
            if (cell == GameState.DOT) {
                gameState.setCell(newX, newY, GameState.EMPTY);
                gameState.addScore(DOT_SCORE);
            } else if (cell == GameState.POWER_PELLET) {
                gameState.setCell(newX, newY, GameState.EMPTY);
                gameState.addScore(POWER_PELLET_SCORE);
                // Make all ghosts vulnerable
                for (GhostImpl ghost : gameState.getGhosts()) {
                    ghost.setVulnerableTime(10.0); // 10 seconds
                }
            }
            
            // Check collision with ghosts
            checkGhostCollisions();
        }
        
        updateGame();
    }
    
    private void checkGhostCollisions() {
        int pacX = gameState.getPacmanX();
        int pacY = gameState.getPacmanY();
        
        for (GhostImpl ghost : gameState.getGhosts()) {
            if (ghost.getX() == pacX && ghost.getY() == pacY) {
                if (ghost.isVulnerable()) {
                    // Eat ghost
                    ghost.setPosition(-100, -100); // Move off screen
                    gameState.addScore(GHOST_SCORE);
                } else {
                    // Lose life
                    gameState.loseLife();
                    if (gameState.getLives() <= 0) {
                        gameState.setStatus(LOSE);
                    } else {
                        // Reset positions
                        gameState.setPacmanPosition(1, 1);
                    }
                }
            }
        }
    }
    
    private void updateGame() {
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0;
        lastUpdateTime = currentTime;
        
        // Update ghost vulnerable times
        for (GhostImpl ghost : gameState.getGhosts()) {
            ghost.updateVulnerableTime(deltaTime);
        }
        
        // Simple ghost AI - move randomly
        moveGhosts();
        
        // Check win condition
        if (allDotsEaten()) {
            gameState.setStatus(WIN);
        }
    }
    
    private void moveGhosts() {
        // Simple random movement for ghosts
        for (GhostImpl ghost : gameState.getGhosts()) {
            if (random.nextDouble() < 0.3) { // 30% chance to move
                int dir = random.nextInt(4);
                int dx = 0, dy = 0;
                switch (dir) {
                    case 0: dy = -1; break; // UP
                    case 1: dy = 1; break;  // DOWN
                    case 2: dx = -1; break; // LEFT
                    case 3: dx = 1; break;  // RIGHT
                }
                
                int newX = gameState.wrapX(ghost.getX() + dx);
                int newY = gameState.wrapY(ghost.getY() + dy);
                
                if (gameState.isValidPosition(newX, newY)) {
                    ghost.setPosition(newX, newY);
                }
            }
        }
    }
    
    private boolean allDotsEaten() {
        for (int x = 0; x < gameState.getWidth(); x++) {
            for (int y = 0; y < gameState.getHeight(); y++) {
                int cell = gameState.getCell(x, y);
                if (cell == GameState.DOT || cell == GameState.POWER_PELLET) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void play() {
        if (gameState.getStatus() == NOT_STARTED || gameState.getStatus() == PAUSED) {
            gameState.setStatus(RUNNING);
            lastUpdateTime = System.currentTimeMillis();
        } else if (gameState.getStatus() == RUNNING) {
            gameState.setStatus(PAUSED);
        }
    }
    
    @Override
    public void end(int code) {
        gameState.setStatus(DONE);
    }
    
    @Override
    public Character getKeyChar() {
        Character key = lastKeyChar;
        lastKeyChar = null; // Clear after reading
        return key;
    }
    
    public void setKeyChar(Character key) {
        this.lastKeyChar = key;
    }
    
    public GameState getGameState() {
        return gameState;
    }
}
