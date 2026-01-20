package server;

/**
 * Interface for Pacman game - used by Ex3Main and Ex3Algo
 */
public interface PacmanGame {
    // Direction constants
    int UP = 0;
    int LEFT = 1;
    int DOWN = 2;
    int RIGHT = 3;
    int ERR = -1;
    
    // Status constants
    int NOT_STARTED = 0;
    int RUNNING = 1;
    int PAUSED = 2;
    int DONE = 3;
    int WIN = 4;
    int LOSE = 5;
    
    /**
     * Gets the game board as a 2D integer array
     */
    int[][] getGame(int code);
    
    /**
     * Gets the position of an entity as "x,y" string
     */
    String getPos(int code);
    
    /**
     * Gets all ghosts in the game
     */
    GhostCL[] getGhosts(int code);
    
    /**
     * Gets the current game status
     */
    int getStatus();
    
    /**
     * Moves an entity in the given direction
     */
    void move(int dir);
    
    /**
     * Starts/continues the game
     */
    void play();
    
    /**
     * Ends the game
     */
    void end(int code);
}
