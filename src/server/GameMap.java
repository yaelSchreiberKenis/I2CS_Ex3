package server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Game-specific map implementation for pathfinding.
 * Simplified version of Map class for use by the game server.
 */
public class GameMap {
    private int[][] _map;
    private boolean _cyclicFlag = true;
    
    public GameMap(int w, int h, int v) {
        _map = new int[h][w];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                _map[y][x] = v;
            }
        }
    }
    
    public GameMap(int[][] data) {
        _map = new int[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            System.arraycopy(data[i], 0, _map[i], 0, data[i].length);
        }
    }
    
    public int getWidth() { return _map[0].length; }
    public int getHeight() { return _map.length; }
    
    public int getPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
            return -1;
        }
        return _map[y][x];
    }
    
    public int getPixel(Pixel2D p) {
        return getPixel(p.getX(), p.getY());
    }
    
    public void setCyclic(boolean cy) { _cyclicFlag = cy; }
    public boolean isCyclic() { return _cyclicFlag; }
    
    /**
     * Compute shortest path between two points avoiding obstacles.
     */
    public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        if (getPixel(p1) == obsColor || getPixel(p2) == obsColor) {
            return null;
        }
        
        int width = getWidth();
        int height = getHeight();
        boolean[][] visited = new boolean[height][width];
        Pixel2D[][] parent = new Pixel2D[height][width];
        
        Queue<Pixel2D> queue = new LinkedList<>();
        queue.offer(p1);
        visited[p1.getY()][p1.getX()] = true;
        
        while (!queue.isEmpty()) {
            Pixel2D current = queue.poll();
            
            if (current.getX() == p2.getX() && current.getY() == p2.getY()) {
                // Reconstruct path
                ArrayList<Pixel2D> path = new ArrayList<>();
                Pixel2D node = current;
                while (node != null) {
                    path.add(0, node);
                    node = parent[node.getY()][node.getX()];
                }
                return path.toArray(new Pixel2D[0]);
            }
            
            for (Pixel2D neighbor : getNeighbors(current)) {
                int nx = neighbor.getX();
                int ny = neighbor.getY();
                
                if (!visited[ny][nx] && getPixel(nx, ny) != obsColor) {
                    visited[ny][nx] = true;
                    parent[ny][nx] = current;
                    queue.offer(neighbor);
                }
            }
        }
        return null;
    }
    
    private ArrayList<Pixel2D> getNeighbors(Pixel2D p) {
        ArrayList<Pixel2D> neighbors = new ArrayList<>();
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        
        for (int[] d : dirs) {
            int nx = p.getX() + d[0];
            int ny = p.getY() + d[1];
            
            if (_cyclicFlag) {
                if (nx < 0) nx = getWidth() - 1;
                else if (nx >= getWidth()) nx = 0;
                if (ny < 0) ny = getHeight() - 1;
                else if (ny >= getHeight()) ny = 0;
                neighbors.add(new Index2D(nx, ny));
            } else {
                if (nx >= 0 && nx < getWidth() && ny >= 0 && ny < getHeight()) {
                    neighbors.add(new Index2D(nx, ny));
                }
            }
        }
        return neighbors;
    }
}
