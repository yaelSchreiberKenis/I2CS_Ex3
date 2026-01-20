import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import server.GameState;
import server.GhostImpl;

/**
 * JUnit tests for GhostImpl class
 */
public class GhostImplTest {
    
    private GameState gameState;
    private GhostImpl ghost;
    
    @Before
    public void setUp() {
        gameState = new GameState(true, 50);
        ghost = new GhostImpl(5, 10, gameState);
    }
    
    // ==================== Position Tests ====================
    
    @Test
    public void testInitialPosition() {
        assertEquals("Initial X should be 5", 5, ghost.getX());
        assertEquals("Initial Y should be 10", 10, ghost.getY());
    }
    
    @Test
    public void testSetPosition() {
        ghost.setPosition(15, 20);
        assertEquals("X should be 15", 15, ghost.getX());
        assertEquals("Y should be 20", 20, ghost.getY());
    }
    
    @Test
    public void testGetPosFormat() {
        String pos = ghost.getPos(0);
        assertEquals("Position string should be 'x,y' format", "5,10", pos);
    }
    
    @Test
    public void testGetPosAfterMove() {
        ghost.setPosition(7, 12);
        String pos = ghost.getPos(0);
        assertEquals("Position string should update after move", "7,12", pos);
    }
    
    // ==================== Vulnerability Tests ====================
    
    @Test
    public void testInitialVulnerability() {
        assertFalse("Ghost should not be vulnerable initially", ghost.isVulnerable());
    }
    
    @Test
    public void testSetVulnerable() {
        ghost.setVulnerableTime(100);
        assertTrue("Ghost should be vulnerable after setting time", ghost.isVulnerable());
    }
    
    @Test
    public void testRemainTimeAsEatable() {
        ghost.setVulnerableTime(100); // Sets to 100 * dt = 5000
        double time = ghost.remainTimeAsEatable(0);
        assertEquals("Remain time should be 5000", 5000.0, time, 0.001);
    }
    
    @Test
    public void testSharedVulnerability() {
        // Create two ghosts sharing the same GameState
        GhostImpl ghost2 = new GhostImpl(8, 8, gameState);
        
        // Make one ghost vulnerable
        ghost.setVulnerableTime(100);
        
        // Both should be vulnerable (shared timer)
        assertTrue("Ghost 1 should be vulnerable", ghost.isVulnerable());
        assertTrue("Ghost 2 should also be vulnerable (shared)", ghost2.isVulnerable());
        assertEquals("Both ghosts should have same remain time", 
            ghost.remainTimeAsEatable(0), ghost2.remainTimeAsEatable(0), 0.001);
    }
    
    @Test
    public void testVulnerabilityDecreases() {
        ghost.setVulnerableTime(2); // 2 * 50 = 100
        gameState.decreaseSharedVulnerableTime(); // -50 -> 50
        
        double remaining = ghost.remainTimeAsEatable(0);
        assertEquals("Remaining time should decrease", 50.0, remaining, 0.001);
        assertTrue("Ghost should still be vulnerable", ghost.isVulnerable());
    }
    
    @Test
    public void testVulnerabilityExpires() {
        ghost.setVulnerableTime(1); // 1 * 50 = 50
        gameState.decreaseSharedVulnerableTime(); // -50 -> 0
        
        assertFalse("Ghost should not be vulnerable after time expires", ghost.isVulnerable());
        assertEquals("Remaining time should be 0", 0.0, ghost.remainTimeAsEatable(0), 0.001);
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    public void testNegativePosition() {
        // Some games might allow negative positions for off-screen ghosts
        ghost.setPosition(-1, -1);
        assertEquals("X should be -1", -1, ghost.getX());
        assertEquals("Y should be -1", -1, ghost.getY());
    }
    
    @Test
    public void testZeroPosition() {
        ghost.setPosition(0, 0);
        assertEquals("X should be 0", 0, ghost.getX());
        assertEquals("Y should be 0", 0, ghost.getY());
        assertEquals("Position string should be '0,0'", "0,0", ghost.getPos(0));
    }
    
    @Test
    public void testLargePosition() {
        ghost.setPosition(1000, 2000);
        assertEquals("X should handle large values", 1000, ghost.getX());
        assertEquals("Y should handle large values", 2000, ghost.getY());
    }
    
    @Test
    public void testCodeParameterIgnored() {
        // The code parameter in getPos and remainTimeAsEatable should be ignored
        // (it's part of the interface but not used in this implementation)
        ghost.setVulnerableTime(100);
        
        assertEquals("Code 0 should work", ghost.remainTimeAsEatable(0), ghost.remainTimeAsEatable(1), 0.001);
        assertEquals("getPos with different codes should be same", ghost.getPos(0), ghost.getPos(99));
    }
}
