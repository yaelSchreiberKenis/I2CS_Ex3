/**
 * Interface for ghost entities in the game
 */
public interface GhostInterface {
    /**
     * Gets the ghost's position as "x,y" string
     * @param code entity code
     * @return position string "x,y"
     */
    String getPos(int code);
    
    /**
     * Gets the remaining time the ghost is vulnerable (eatable)
     * @param code entity code
     * @return remaining time as double (0 or less means not vulnerable)
     */
    double remainTimeAsEatable(int code);
}
