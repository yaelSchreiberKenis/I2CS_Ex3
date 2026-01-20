package server;

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
    private int dt;
    private List<GhostImpl> ghosts;
    private int score;
    private int status; // NOT_STARTED, RUNNING, PAUSED, DONE, etc.
    private boolean cyclicMode;
    private double sharedVulnerableTime = 0; // Shared vulnerability timer for all ghosts
    
    // Game constants
    public static final int EMPTY = 0;
    public static final int WALL = -1;
    public static final int DOT = 1;
    public static final int POWER_PELLET = 2;
    
    public GameState(boolean cyclic, int dt) {
        this.ghosts = new ArrayList<>();
        this.score = 0;
        this.status = PacmanGame.NOT_STARTED;
        this.cyclicMode = cyclic;
        this.dt = dt;
        initializeBoard();
    }
    
    private void initializeBoard() {
        // Board: 22 rows x 23 columns
        // board[x][y] stores position (x,y)
        this.board = new int[][] {
            { WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, WALL, WALL, DOT, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, DOT, WALL, WALL, WALL },
            { WALL, DOT, DOT, POWER_PELLET, WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL, POWER_PELLET, DOT, DOT, WALL },
            { WALL, DOT, WALL, WALL, WALL, DOT, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, DOT, WALL, WALL, WALL, DOT, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, WALL, WALL, DOT, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, DOT, WALL, WALL, WALL },
            { DOT, DOT, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, DOT, DOT },
            { WALL, WALL, DOT, WALL, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, WALL, DOT, WALL, WALL },
            { DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT },
            { WALL, WALL, DOT, WALL, WALL, DOT, WALL, DOT, WALL, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, WALL, DOT, WALL, DOT, WALL, WALL, DOT, WALL, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, WALL, DOT, WALL, WALL, WALL, EMPTY, WALL, WALL, WALL, DOT, WALL, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, DOT, DOT, DOT, EMPTY, DOT, DOT, DOT, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, DOT, WALL, DOT, WALL, DOT, WALL, DOT, DOT, DOT, DOT, WALL, DOT, DOT, DOT, DOT, WALL, DOT, WALL, DOT, WALL, DOT, WALL },
            { WALL, DOT, WALL, DOT, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, DOT, WALL, DOT, WALL },
            { WALL, DOT, WALL, POWER_PELLET, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL, POWER_PELLET, WALL, DOT, WALL },
            { WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL, WALL, WALL, WALL, WALL, WALL, DOT, WALL, DOT, WALL },
            { WALL, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, DOT, WALL },
            { WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL }
        };
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
        return board.length; // Width is number of rows (22)
    }
    
    public int getHeight() {
        return board[0].length; // Height is number of columns (23)
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
    
    public double getSharedVulnerableTime() {
        return sharedVulnerableTime;
    }
    
    public void setSharedVulnerableTime(double moves) {
        this.sharedVulnerableTime = moves * dt;
    }
    
    public void decreaseSharedVulnerableTime() {
        // Update vulnerability based on moves (not time)
        // Each call decrements by the given number of moves
        if (sharedVulnerableTime > 0) {
            sharedVulnerableTime = Math.max(0, sharedVulnerableTime - dt);
        }
    }
}
