import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import server.GameState;
import server.GhostImpl;
import server.PacmanGame;

/**
 * JUnit tests for GameState class
 */
public class GameStateTest {
    
    private GameState gameState;
    
    @Before
    public void setUp() {
        gameState = new GameState(true, 50); // Cyclic mode, dt=50
    }
    
    // ==================== Board Tests ====================
    
    @Test
    public void testBoardDimensions() {
        assertEquals("Board width should be 22", 22, gameState.getWidth());
        assertEquals("Board height should be 23", 23, gameState.getHeight());
    }
    
    @Test
    public void testBoardCopyIsIndependent() {
        int[][] board1 = gameState.getBoard();
        int[][] board2 = gameState.getBoard();
        
        // Modify board1
        board1[1][1] = 999;
        
        // board2 should not be affected
        assertNotEquals("Board copy should be independent", 999, board2[1][1]);
    }
    
    @Test
    public void testBoardBorders() {
        // All borders should be walls
        for (int y = 0; y < gameState.getHeight(); y++) {
            assertEquals("Left border should be wall", GameState.WALL, gameState.getCell(0, y));
            assertEquals("Right border should be wall", GameState.WALL, gameState.getCell(gameState.getWidth() - 1, y));
        }
        for (int x = 0; x < gameState.getWidth(); x++) {
            assertEquals("Top border should be wall", GameState.WALL, gameState.getCell(x, 0));
            assertEquals("Bottom border should be wall", GameState.WALL, gameState.getCell(x, gameState.getHeight() - 1));
        }
    }
    
    // ==================== Pacman Position Tests ====================
    
    @Test
    public void testSetPacmanPosition() {
        gameState.setPacmanPosition(5, 10);
        assertEquals("Pacman X should be 5", 5, gameState.getPacmanX());
        assertEquals("Pacman Y should be 10", 10, gameState.getPacmanY());
    }
    
    @Test
    public void testPacmanPositionUpdate() {
        gameState.setPacmanPosition(1, 1);
        gameState.setPacmanPosition(3, 5);
        assertEquals("Pacman X should update to 3", 3, gameState.getPacmanX());
        assertEquals("Pacman Y should update to 5", 5, gameState.getPacmanY());
    }
    
    // ==================== Cell Tests ====================
    
    @Test
    public void testGetCellOutOfBounds() {
        assertEquals("Out of bounds should return WALL", GameState.WALL, gameState.getCell(-1, 0));
        assertEquals("Out of bounds should return WALL", GameState.WALL, gameState.getCell(0, -1));
        assertEquals("Out of bounds should return WALL", GameState.WALL, gameState.getCell(100, 0));
        assertEquals("Out of bounds should return WALL", GameState.WALL, gameState.getCell(0, 100));
    }
    
    @Test
    public void testSetCell() {
        // Find a non-wall cell to test
        int testX = 1, testY = 1;
        int originalValue = gameState.getCell(testX, testY);
        
        gameState.setCell(testX, testY, GameState.POWER_PELLET);
        assertEquals("Cell should be updated", GameState.POWER_PELLET, gameState.getCell(testX, testY));
        
        // Restore original value
        gameState.setCell(testX, testY, originalValue);
    }
    
    @Test
    public void testSetCellOutOfBounds() {
        // Should not throw exception
        gameState.setCell(-1, 0, GameState.DOT);
        gameState.setCell(0, -1, GameState.DOT);
        gameState.setCell(100, 0, GameState.DOT);
    }
    
    // ==================== Score Tests ====================
    
    @Test
    public void testInitialScore() {
        assertEquals("Initial score should be 0", 0, gameState.getScore());
    }
    
    @Test
    public void testAddScore() {
        gameState.addScore(10);
        assertEquals("Score should be 10", 10, gameState.getScore());
        
        gameState.addScore(20);
        assertEquals("Score should be 30", 30, gameState.getScore());
    }
    
    // ==================== Status Tests ====================
    
    @Test
    public void testInitialStatus() {
        assertEquals("Initial status should be NOT_STARTED", PacmanGame.NOT_STARTED, gameState.getStatus());
    }
    
    @Test
    public void testSetStatus() {
        gameState.setStatus(PacmanGame.RUNNING);
        assertEquals("Status should be RUNNING", PacmanGame.RUNNING, gameState.getStatus());
        
        gameState.setStatus(PacmanGame.DONE);
        assertEquals("Status should be DONE", PacmanGame.DONE, gameState.getStatus());
    }
    
    // ==================== Cyclic Mode Tests ====================
    
    @Test
    public void testCyclicMode() {
        assertTrue("Should be in cyclic mode", gameState.isCyclicMode());
        
        GameState nonCyclic = new GameState(false, 50);
        assertFalse("Should not be in cyclic mode", nonCyclic.isCyclicMode());
    }
    
    @Test
    public void testWrapXCyclic() {
        // Test wrapping from left edge
        assertEquals("Should wrap from -1 to width-1", gameState.getWidth() - 1, gameState.wrapX(-1));
        
        // Test wrapping from right edge
        assertEquals("Should wrap from width to 0", 0, gameState.wrapX(gameState.getWidth()));
        
        // Test no wrapping for valid positions
        assertEquals("Should not wrap valid position", 5, gameState.wrapX(5));
    }
    
    @Test
    public void testWrapYCyclic() {
        // Test wrapping from top edge
        assertEquals("Should wrap from -1 to height-1", gameState.getHeight() - 1, gameState.wrapY(-1));
        
        // Test wrapping from bottom edge
        assertEquals("Should wrap from height to 0", 0, gameState.wrapY(gameState.getHeight()));
        
        // Test no wrapping for valid positions
        assertEquals("Should not wrap valid position", 10, gameState.wrapY(10));
    }
    
    @Test
    public void testWrapNonCyclic() {
        GameState nonCyclic = new GameState(false, 50);
        
        // In non-cyclic mode, wrap should not change the value
        assertEquals("Should not wrap in non-cyclic mode", -1, nonCyclic.wrapX(-1));
        assertEquals("Should not wrap in non-cyclic mode", 100, nonCyclic.wrapX(100));
    }
    
    // ==================== Valid Position Tests ====================
    
    @Test
    public void testIsValidPositionCyclic() {
        // In cyclic mode, all positions are technically valid (wrapping)
        assertTrue("All positions valid in cyclic mode", gameState.isValidPosition(-1, 0));
        assertTrue("All positions valid in cyclic mode", gameState.isValidPosition(100, 100));
    }
    
    @Test
    public void testIsValidPositionNonCyclic() {
        GameState nonCyclic = new GameState(false, 50);
        
        assertFalse("Negative position should be invalid", nonCyclic.isValidPosition(-1, 0));
        assertFalse("Position beyond bounds should be invalid", nonCyclic.isValidPosition(100, 0));
        
        // Wall position should also be invalid
        assertFalse("Wall position should be invalid", nonCyclic.isValidPosition(0, 0));
    }
    
    // ==================== Ghost Tests ====================
    
    @Test
    public void testAddGhost() {
        assertEquals("Initial ghost count should be 0", 0, gameState.getGhosts().size());
        
        GhostImpl ghost = new GhostImpl(5, 5, gameState);
        gameState.addGhost(ghost);
        
        assertEquals("Ghost count should be 1", 1, gameState.getGhosts().size());
    }
    
    @Test
    public void testGhostListIsDefensiveCopy() {
        GhostImpl ghost = new GhostImpl(5, 5, gameState);
        gameState.addGhost(ghost);
        
        // Get the list and try to modify it
        gameState.getGhosts().clear();
        
        // Original list should not be affected
        assertEquals("Ghost list should be defensive copy", 1, gameState.getGhosts().size());
    }
    
    // ==================== Vulnerability Timer Tests ====================
    
    @Test
    public void testInitialVulnerableTime() {
        assertEquals("Initial vulnerable time should be 0", 0.0, gameState.getSharedVulnerableTime(), 0.001);
    }
    
    @Test
    public void testSetVulnerableTime() {
        gameState.setSharedVulnerableTime(100);
        // Time is multiplied by dt (50)
        assertEquals("Vulnerable time should be 5000", 5000.0, gameState.getSharedVulnerableTime(), 0.001);
    }
    
    @Test
    public void testDecreaseVulnerableTime() {
        gameState.setSharedVulnerableTime(100); // 5000
        gameState.decreaseSharedVulnerableTime(); // Decreases by dt (50)
        assertEquals("Vulnerable time should decrease", 4950.0, gameState.getSharedVulnerableTime(), 0.001);
    }
    
    @Test
    public void testVulnerableTimeDoesNotGoBelowZero() {
        gameState.setSharedVulnerableTime(1); // 50
        gameState.decreaseSharedVulnerableTime(); // -50 -> 0
        gameState.decreaseSharedVulnerableTime(); // Should stay at 0
        assertEquals("Vulnerable time should not go below 0", 0.0, gameState.getSharedVulnerableTime(), 0.001);
    }
    
    // ==================== Constants Tests ====================
    
    @Test
    public void testConstants() {
        assertEquals("EMPTY should be 0", 0, GameState.EMPTY);
        assertEquals("WALL should be -1", -1, GameState.WALL);
        assertEquals("DOT should be 1", 1, GameState.DOT);
        assertEquals("POWER_PELLET should be 2", 2, GameState.POWER_PELLET);
    }
}
