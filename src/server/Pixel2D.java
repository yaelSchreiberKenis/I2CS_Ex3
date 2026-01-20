package server;

/**
 * This interface represents an integer based coordinate of a 2D raster.
 */
public interface Pixel2D {
    int getX();
    int getY();
    double distance2D(Pixel2D p2);
}
