import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for Map using JUnit
 */
public class MapTest {
    
    @Test
    public void testConstructors() {
        // Test constructor with dimensions and initial value
        Map m1 = new Map(5, 10, 42);
        assertEquals("Width should be 5", 5, m1.getWidth());
        assertEquals("Height should be 10", 10, m1.getHeight());
        assertEquals("Initial value should be 42", 42, m1.getPixel(0, 0));
        assertEquals("All pixels should have initial value 42", 42, m1.getPixel(4, 9));
        
        // Test square constructor
        Map m2 = new Map(7);
        assertEquals("Width should be 7", 7, m2.getWidth());
        assertEquals("Height should be 7", 7, m2.getHeight());
        assertEquals("Initial value should be 0", 0, m2.getPixel(0, 0));
        
        // Test constructor from array
        int[][] data = {{1, 2, 3}, {4, 5, 6}};
        Map m3 = new Map(data);
        assertEquals("Width should be 3", 3, m3.getWidth());
        assertEquals("Height should be 2", 2, m3.getHeight());
        assertEquals("Pixel (0,0) should be 1", 1, m3.getPixel(0, 0));
        assertEquals("Pixel (2,0) should be 3", 3, m3.getPixel(2, 0));
        assertEquals("Pixel (0,1) should be 4", 4, m3.getPixel(0, 1));
    }
    
    @Test
    public void testInit() {
        Map m = new Map(3, 4, 0);
        
        // Test init with new dimensions
        m.init(5, 6, 99);
        assertEquals("Width should be 5 after init", 5, m.getWidth());
        assertEquals("Height should be 6 after init", 6, m.getHeight());
        assertEquals("Initial value should be 99", 99, m.getPixel(0, 0));
        
        // Test init with array
        int[][] arr = {{10, 20}, {30, 40}};
        m.init(arr);
        assertEquals("Width should be 2", 2, m.getWidth());
        assertEquals("Height should be 2", 2, m.getHeight());
        assertEquals("Pixel (0,0) should be 10", 10, m.getPixel(0, 0));
        assertEquals("Pixel (1,1) should be 40", 40, m.getPixel(1, 1));
    }
    
    @Test
    public void testGetWidthHeight() {
        Map m = new Map(8, 12, 0);
        assertEquals("getWidth() should return 8", 8, m.getWidth());
        assertEquals("getHeight() should return 12", 12, m.getHeight());
    }
    
    @Test
    public void testGetPixel() {
        Map m = new Map(3, 3, 0);
        
        // Test valid coordinates
        m.setPixel(1, 1, 50);
        assertEquals("Pixel (1,1) should be 50", 50, m.getPixel(1, 1));
        assertEquals("Pixel (0,0) should be 0", 0, m.getPixel(0, 0));
        
        // Test with Pixel2D
        Index2D p = new Index2D(2, 2);
        m.setPixel(p, 100);
        assertEquals("Pixel at Index2D(2,2) should be 100", 100, m.getPixel(p));
        assertEquals("Pixel (2,2) should be 100", 100, m.getPixel(2, 2));
        
        // Test out of bounds
        assertEquals("Out of bounds should return -1", -1, m.getPixel(-1, 0));
        assertEquals("Out of bounds should return -1", -1, m.getPixel(0, -1));
        assertEquals("Out of bounds should return -1", -1, m.getPixel(3, 0));
        assertEquals("Out of bounds should return -1", -1, m.getPixel(0, 3));
    }
    
    @Test
    public void testSetPixel() {
        Map m = new Map(4, 4, 0);
        
        // Test setting with coordinates
        m.setPixel(1, 2, 77);
        assertEquals("Pixel should be set to 77", 77, m.getPixel(1, 2));
        
        // Test setting with Pixel2D
        Index2D p = new Index2D(3, 3);
        m.setPixel(p, 88);
        assertEquals("Pixel should be set to 88", 88, m.getPixel(p));
    }
    
    @Test
    public void testIsInside() {
        Map m = new Map(5, 5, 0);
        
        // Test valid positions
        assertTrue("(0,0) should be inside", m.isInside(new Index2D(0, 0)));
        assertTrue("(4,4) should be inside", m.isInside(new Index2D(4, 4)));
        assertTrue("(2,3) should be inside", m.isInside(new Index2D(2, 3)));
        
        // Test invalid positions
        assertFalse("(-1,0) should not be inside", m.isInside(new Index2D(-1, 0)));
        assertFalse("(0,-1) should not be inside", m.isInside(new Index2D(0, -1)));
        assertFalse("(5,0) should not be inside", m.isInside(new Index2D(5, 0)));
        assertFalse("(0,5) should not be inside", m.isInside(new Index2D(0, 5)));
        assertFalse("(10,10) should not be inside", m.isInside(new Index2D(10, 10)));
    }
    
    @Test
    public void testCyclicMode() {
        Map m = new Map(5, 5, 0);
        
        // Test default (should be true)
        assertTrue("Default cyclic mode should be true", m.isCyclic());
        
        // Test setting cyclic mode
        m.setCyclic(false);
        assertFalse("Cyclic mode should be false", m.isCyclic());
        
        m.setCyclic(true);
        assertTrue("Cyclic mode should be true", m.isCyclic());
    }
    
    @Test
    public void testShortestPath() {
        // Create a simple 3x3 map with no obstacles
        Map m = new Map(3, 3, 0);
        m.setCyclic(false);
        
        int obsColor = -1; // No obstacles
        
        // Test path from (0,0) to (2,2)
        Index2D start = new Index2D(0, 0);
        Index2D end = new Index2D(2, 2);
        Pixel2D[] path = m.shortestPath(start, end, obsColor);
        
        assertNotNull("Path should not be null", path);
        assertEquals("Path length should be 5 (4 steps)", 5, path.length);
        assertTrue("Path should start at (0,0)", path[0].equals(start));
        assertTrue("Path should end at (2,2)", path[path.length - 1].equals(end));
        
        // Test path to same position
        Pixel2D[] selfPath = m.shortestPath(start, start, obsColor);
        assertNotNull("Self path should not be null", selfPath);
        assertTrue("Self path should have at least 1 element", selfPath.length >= 1);
        assertTrue("Self path should start at start position", selfPath[0].equals(start));
        
        // Test path with obstacle
        m.setPixel(1, 0, 1); // Set obstacle at (1,0)
        obsColor = 1;
        Pixel2D[] pathWithObstacle = m.shortestPath(start, end, obsColor);
        assertNotNull("Path should exist even with obstacle", pathWithObstacle);
    }
    
    @Test
    public void testAllDistance() {
        // Create a simple map
        Map m = new Map(4, 4, 0);
        m.setCyclic(false);
        
        int obsColor = -1; // No obstacles
        
        Index2D start = new Index2D(0, 0);
        Map2D distances = m.allDistance(start, obsColor);
        
        assertNotNull("Distances should not be null", distances);
        assertEquals("Start position distance should be 0", 0, distances.getPixel(start));
        assertEquals("Distance to (1,0) should be 1", 1, distances.getPixel(new Index2D(1, 0)));
        assertEquals("Distance to (0,1) should be 1", 1, distances.getPixel(new Index2D(0, 1)));
        assertEquals("Distance to (1,1) should be 2", 2, distances.getPixel(new Index2D(1, 1)));
        assertEquals("Distance to (3,3) should be 6", 6, distances.getPixel(new Index2D(3, 3)));
        
        // Test with obstacle
        m.setPixel(1, 0, 1);
        obsColor = 1;
        Map2D distancesWithObstacle = m.allDistance(start, obsColor);
        assertEquals("Obstacle should have distance -1", -1, distancesWithObstacle.getPixel(new Index2D(1, 0)));
        assertEquals("Should still reach (0,1)", 1, distancesWithObstacle.getPixel(new Index2D(0, 1)));
    }
    
    @Test
    public void testFill() {
        // Create a map with a region to fill
        Map m = new Map(5, 5, 0);
        m.setCyclic(false);
        
        // Create a region of value 1
        m.setPixel(1, 1, 1);
        m.setPixel(2, 1, 1);
        m.setPixel(1, 2, 1);
        m.setPixel(2, 2, 1);
        
        // Fill region starting from (1,1) with value 2
        int filled = m.fill(new Index2D(1, 1), 2);
        assertEquals("Should fill 4 pixels", 4, filled);
        assertEquals("Pixel (1,1) should be filled with 2", 2, m.getPixel(1, 1));
        assertEquals("Pixel (2,1) should be filled with 2", 2, m.getPixel(2, 1));
        assertEquals("Pixel (1,2) should be filled with 2", 2, m.getPixel(1, 2));
        assertEquals("Pixel (2,2) should be filled with 2", 2, m.getPixel(2, 2));
        assertEquals("Pixel (0,0) should remain 0", 0, m.getPixel(0, 0));
        
        // Test filling with same color
        int filledSame = m.fill(new Index2D(1, 1), 2);
        assertEquals("Filling with same color should return 0", 0, filledSame);
    }
}
