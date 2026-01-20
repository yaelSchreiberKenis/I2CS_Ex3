import client.Index2D;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test class for Index2D using JUnit
 */
public class Index2DTest {
    
    @Test
    public void testConstructors() {
        // Test default constructor
        Index2D p1 = new Index2D();
        assertEquals("Default constructor X should be 0", 0, p1.getX());
        assertEquals("Default constructor Y should be 0", 0, p1.getY());
        
        // Test parameterized constructor
        Index2D p2 = new Index2D(5, 10);
        assertEquals("X should be 5", 5, p2.getX());
        assertEquals("Y should be 10", 10, p2.getY());
        
        // Test copy constructor
        Index2D p3 = new Index2D(p2);
        assertEquals("Copy constructor X should be 5", 5, p3.getX());
        assertEquals("Copy constructor Y should be 10", 10, p3.getY());
        assertTrue("Copied Index2D should equal original", p3.equals(p2));
    }
    
    @Test
    public void testGetters() {
        Index2D p = new Index2D(7, 3);
        assertEquals("getX() should return 7", 7, p.getX());
        assertEquals("getY() should return 3", 3, p.getY());
    }
    
    @Test
    public void testDistance2D() {
        Index2D p1 = new Index2D(0, 0);
        Index2D p2 = new Index2D(3, 4);
        double dist = p1.distance2D(p2);
        assertEquals("Distance between (0,0) and (3,4) should be 5.0", 5.0, dist, 0.0001);
        
        // Test distance to self
        double distToSelf = p1.distance2D(p1);
        assertEquals("Distance to self should be 0.0", 0.0, distToSelf, 0.0001);
        
        // Test horizontal distance
        Index2D p3 = new Index2D(0, 0);
        Index2D p4 = new Index2D(5, 0);
        double horizontalDist = p3.distance2D(p4);
        assertEquals("Horizontal distance should be 5.0", 5.0, horizontalDist, 0.0001);
        
        // Test vertical distance
        Index2D p5 = new Index2D(0, 0);
        Index2D p6 = new Index2D(0, 8);
        double verticalDist = p5.distance2D(p6);
        assertEquals("Vertical distance should be 8.0", 8.0, verticalDist, 0.0001);
    }
    
    @Test
    public void testEquals() {
        Index2D p1 = new Index2D(5, 5);
        Index2D p2 = new Index2D(5, 5);
        Index2D p3 = new Index2D(5, 6);
        Index2D p4 = new Index2D(6, 5);
        
        // Test equality with same coordinates
        assertTrue("Index2D with same coordinates should be equal", p1.equals(p2));
        assertTrue("Equals should be symmetric", p2.equals(p1));
        
        // Test inequality with different coordinates
        assertFalse("Index2D with different Y should not be equal", p1.equals(p3));
        assertFalse("Index2D with different X should not be equal", p1.equals(p4));
        
        // Test equality with self
        assertTrue("Index2D should equal itself", p1.equals(p1));
        
        // Test equality with copy
        Index2D p5 = new Index2D(p1);
        assertTrue("Index2D should equal its copy", p1.equals(p5));
    }
    
    @Test
    public void testToString() {
        Index2D p = new Index2D(3, 7);
        String str = p.toString();
        assertEquals("toString() should return '3,7'", "3,7", str);
        
        Index2D p2 = new Index2D(0, 0);
        String str2 = p2.toString();
        assertEquals("toString() should return '0,0'", "0,0", str2);
    }
}
