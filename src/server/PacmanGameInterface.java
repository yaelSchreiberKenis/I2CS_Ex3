/**
 * Interface for Pacman game - matches the original PacmanGame interface
 * Used by game algorithms to interact with the game
 */
public interface PacmanGameInterface {
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
     * @param code player/entity code
     * @return 2D array representing the game board
     */
    int[][] getGame(int code);
    
    /**
     * Gets the position of an entity as "x,y" string
     * @param code entity code (0 for Pacman)
     * @return position string "x,y"
     */
    String getPos(int code);
    
    /**
     * Gets all ghosts in the game
     * @param code player code
     * @return array of ghost objects
     */
    GhostInterface[] getGhosts(int code);
    
    /**
     * Gets the current game status
     * @return status constant (NOT_STARTED, RUNNING, PAUSED, DONE, etc.)
     */
    int getStatus();
    
    /**
     * Moves an entity in the given direction
     * @param dir direction (UP, DOWN, LEFT, RIGHT)
     */
    void move(int dir);
    
    /**
     * Starts/continues the game
     */
    void play();
    
    /**
     * Ends the game
     * @param code exit code
     */
    void end(int code);
    
    /**
     * Gets the last pressed key character
     * @return key character or null
     */
    Character getKeyChar();
}
