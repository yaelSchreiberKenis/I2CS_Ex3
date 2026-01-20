package server;

/**
 * Represents a ghost entity in the Pacman game
 */
public class GhostImpl implements GhostCL {
    private int x;
    private int y;
    private GameState gameState; // Reference to game state for shared vulnerability timer
    
    public GhostImpl(int x, int y, GameState gameState) {
        this.x = x;
        this.y = y;
        this.gameState = gameState;
    }
    
    @Override
    public String getPos(int code) {
        return x + "," + y;
    }
    
    @Override
    public double remainTimeAsEatable(int code) {
        return gameState.getSharedVulnerableTime();
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void setVulnerableTime(double moves) {
        gameState.setSharedVulnerableTime(moves);
    }
    
    public boolean isVulnerable() {
        return gameState.getSharedVulnerableTime() > 0;
    }
}
