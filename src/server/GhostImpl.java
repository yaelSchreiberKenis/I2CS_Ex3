/**
 * Implementation of GhostInterface
 * Represents a ghost entity in the Pacman game
 */
public class GhostImpl implements GhostInterface {
    private int x;
    private int y;
    private double vulnerableTime; // Time remaining as vulnerable (eatable)
    
    public GhostImpl(int x, int y) {
        this.x = x;
        this.y = y;
        this.vulnerableTime = 0;
    }
    
    @Override
    public String getPos(int code) {
        return x + "," + y;
    }
    
    @Override
    public double remainTimeAsEatable(int code) {
        return vulnerableTime;
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
    
    public void setVulnerableTime(double time) {
        this.vulnerableTime = time;
    }
    
    public void updateVulnerableTime(double deltaTime) {
        if (vulnerableTime > 0) {
            vulnerableTime = Math.max(0, vulnerableTime - deltaTime);
        }
    }
    
    public boolean isVulnerable() {
        return vulnerableTime > 0;
    }
}
