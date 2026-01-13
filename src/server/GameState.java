import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the Pacman game
 * Manages the board, positions, score, etc.
 */
public class GameState {
    private int[][] board;
    private int pacmanX;
    private int pacmanY;
    private List<GhostImpl> ghosts;
    private int score;
    private int lives;
    private int status; // NOT_STARTED, RUNNING, PAUSED, DONE, etc.
    private boolean cyclicMode;
    
    // Game constants
    public static final int EMPTY = 0;
    public static final int WALL = -1;
    public static final int DOT = 1;
    public static final int POWER_PELLET = 2;
    
    public GameState(int width, int height, boolean cyclic) {
        this.board = new int[width][height];
        this.ghosts = new ArrayList<>();
        this.score = 0;
        this.lives = 3;
        this.status = PacmanGameInterface.NOT_STARTED;
        this.cyclicMode = cyclic;
        initializeBoard();
    }
    
    private void initializeBoard() {
        // Initialize board with dots
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                board[x][y] = DOT;
            }
        }
    }
    
    public int[][] getBoard() {
        // Return a copy of the board
        int[][] copy = new int[board.length][board[0].length];
        for (int x = 0; x < board.length; x++) {
            System.arraycopy(board[x], 0, copy[x], 0, board[x].length);
        }
        return copy;
    }
    
    public int getWidth() {
        return board.length;
    }
    
    public int getHeight() {
        return board[0].length;
    }
    
    public int getPacmanX() {
        return pacmanX;
    }
    
    public int getPacmanY() {
        return pacmanY;
    }
    
    public void setPacmanPosition(int x, int y) {
        this.pacmanX = x;
        this.pacmanY = y;
    }
    
    public List<GhostImpl> getGhosts() {
        return new ArrayList<>(ghosts);
    }
    
    public void addGhost(GhostImpl ghost) {
        ghosts.add(ghost);
    }
    
    public void setBoard(int[][] board) {
        this.board = board;
    }
    
    public int getScore() {
        return score;
    }
    
    public void addScore(int points) {
        this.score += points;
    }
    
    public int getLives() {
        return lives;
    }
    
    public void loseLife() {
        this.lives--;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public boolean isCyclicMode() {
        return cyclicMode;
    }
    
    public int getCell(int x, int y) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return WALL;
        }
        return board[x][y];
    }
    
    public void setCell(int x, int y, int value) {
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            board[x][y] = value;
        }
    }
    
    public boolean isValidPosition(int x, int y) {
        if (cyclicMode) {
            return true; // In cyclic mode, all positions are valid (wrapping)
        }
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight() && getCell(x, y) != WALL;
    }
    
    public int wrapX(int x) {
        if (!cyclicMode) return x;
        if (x < 0) return getWidth() + x;
        if (x >= getWidth()) return x - getWidth();
        return x;
    }
    
    public int wrapY(int y) {
        if (!cyclicMode) return y;
        if (y < 0) return getHeight() + y;
        if (y >= getHeight()) return y - getHeight();
        return y;
    }
}
