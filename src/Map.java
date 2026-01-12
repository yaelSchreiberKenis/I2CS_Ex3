import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class represents a 2D map as a "screen" or a raster matrix or maze over integers.
 * @author boaz.benmoshe
 *
 */
public class Map implements Map2D {
	private int[][] _map;
	private boolean _cyclicFlag = true;
	
	/**
	 * Constructs a w*h 2D raster map with an init value v.
	 * @param w
	 * @param h
	 * @param v
	 */
	public Map(int w, int h, int v) {init(w,h, v);}
	/**
	 * Constructs a square map (size*size).
	 * @param size
	 */
	public Map(int size) {this(size,size, 0);}
	
	/**
	 * Constructs a map from a given 2D array.
	 * @param data
	 */
	public Map(int[][] data) {
		init(data);
	}

	@Override
	public void init(int w, int h, int v) {
		// Create a new matrix with dimensions [height][width]
		_map = new int[h][w];
		// Iterate through all positions and set them to the initial value v
		// Note: _map indexing is [y][x], so y (row) is the first index
		for (int x = 0; x < w; x++){
			for (int y = 0; y < h; y++){
				_map[y][x] = v;
			}
		}
	}

	@Override
	public void init(int[][] arr) {
		// Create a new matrix with the same dimensions as the input array
		_map = new int[arr.length][arr[0].length];
		// Copy all values from the input array to the internal matrix
		for (int i = 0; i < arr.length; i++){
			for (int j = 0; j < arr[0].length; j++){
				_map[i][j] = arr[i][j];
			}
		}
	}

	@Override
	public int[][] getMap() {
		// Create a new 2D array with the same dimensions
		int[][] ans = new int[_map.length][_map[0].length];
		// Copy all values from the internal _map to the result array
		for (int i = 0; i < ans.length; i++){
			for (int j = 0; j < ans[i].length; j++){
				ans[i][j] = _map[i][j];
			}
		}
		return ans;
	}

	@Override
	public int getWidth() {return _map[0].length;}

	@Override
	public int getHeight() {return _map.length;}

	@Override
	public int getPixel(int x, int y) {
		// Check if coordinates are out of bounds
		// Returns -1 if x is greater than or equal to width, or y is greater than or equal to height
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
			return -1;
		}
		// Return the pixel value (_map is indexed as [y][x])
		return _map[y][x];
	}

	@Override
	public int getPixel(Pixel2D p) {
		if (p.getX() < 0 || p.getY() < 0 || p.getX() >= getWidth() || p.getY() >= getHeight()) {
			return -1;
		}
		return _map[p.getY()][p.getX()];
	}

	@Override
	public void setPixel(int x, int y, int v) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
			return;
		}
		_map[y][x] = v;
	}

	@Override
	public void setPixel(Pixel2D p, int v) {
		if (p.getX() < 0 || p.getY() < 0 || p.getX() >= getWidth() || p.getY() >= getHeight()) {
			return;
		}
		_map[p.getY()][p.getX()] = v;
	}
	@Override
	/** 
	 * Fills this map with the new color (new_v) starting from p.
	 * https://en.wikipedia.org/wiki/Flood_fill
	 */
	public int fill(Pixel2D xy, int new_v) {
		// Optimization: if the new color is the same as the original color, no filling is needed
		if (new_v == _map[xy.getY()][xy.getX()]) {
			return 0;
		}
		// Perform recursive flood fill starting from the given pixel
		// Store the original color to identify which pixels to fill
		return recursiveFill(xy, _map[xy.getY()][xy.getX()], new_v, _cyclicFlag);
	}

	@Override
	/**
	 * BFS like shortest the computation based on iterative raster implementation of BFS, see:
	 * https://en.wikipedia.org/wiki/Breadth-first_search
	 */
	public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
		Pixel2D[] ans = null;  // The result path
		// Create a 2D array of nodes to track visited status and parent relationships
		Node[][] nodeArray = new Node[_map.length][_map[0].length];
		// Initialize all nodes with their corresponding pixel coordinates
		for (int y = 0; y < _map.length; y++) {
			for (int x = 0; x < _map[0].length; x++) {
				nodeArray[y][x] = new Node(new Index2D(x, y));
			}
		}

		// Mark the starting node as visited (with null parent since it's the root)
		nodeArray[p1.getY()][p1.getX()].visit(null);

		// Initialize the BFS queue with the starting node
		Queue<Node> q = new LinkedList<>();
		q.add(new Node(p1));

		// BFS main loop: process nodes level by level
		while (!q.isEmpty()) {
			Node current = q.poll();
			// Skip obstacle pixels
			if (getPixel(current.current) == obsColor) {
				continue;
			}
			// Check if we've reached the destination
			if (current.current.equals(p2)) {
				// Reconstruct the path by following parent pointers backwards
				ArrayList<Pixel2D> path = new ArrayList<>();
				while (current.parent != null) {
					// Add current node to the front of the path
					path.addFirst(current.current);
					// Move to parent node
					current = current.parent;
				}
				// Add the starting node (which has null parent)
				path.addFirst(current.current);
				// Convert ArrayList to array and return
				ans = path.toArray(new Pixel2D[0]);
				return ans;
			}

			// Get all valid neighbors of the current node
			ArrayList<Pixel2D> neighbors = getAllNeighbors((Pixel2D)current.current, _cyclicFlag);
			// Process each neighbor
			for (Pixel2D neighbor : neighbors) {
				// Only process unvisited neighbors
				if (!nodeArray[neighbor.getY()][neighbor.getX()].visited) {
					// Mark neighbor as visited and set current as its parent
					nodeArray[neighbor.getY()][neighbor.getX()].visit(current);
					// Add neighbor to queue for further exploration
					q.add(nodeArray[neighbor.getY()][neighbor.getX()]);
				}
			}
		}
		// No path found
		return null;
	}

	@Override
	public boolean isInside(Pixel2D p) {
		// Check if x is within [0, width) and y is within [0, height)
		return (p.getX() >= 0 && p.getY() >= 0 && p.getX() < getWidth() && p.getY() <  getHeight());
	}

	@Override
	public boolean isCyclic() {
		return _cyclicFlag;
	}

	@Override
	public void setCyclic(boolean cy) {_cyclicFlag = cy;}

	@Override
	/////// add your code below ///////
	public Map2D allDistance(Pixel2D start, int obsColor) {
		// Initialize result map with -1 (unreachable) for all pixels
		Map2D ans = new Map(getWidth(), getHeight(), -1);
		// If starting position is an obstacle, return map with all -1
		if (getPixel(start) == obsColor) {
			return ans;
		}
		// Set starting position distance to 0
		ans.setPixel(start, 0);

		// Initialize BFS queue with starting pixel
		Queue<Pixel2D> q = new LinkedList<>();
		q.add(start);

		// BFS main loop: explore all reachable pixels
		while (!q.isEmpty()) {
			Pixel2D current = q.poll();
			// Get all valid neighbors of current pixel
			ArrayList<Pixel2D> neighbors = getAllNeighbors(current, _cyclicFlag);

			// Process each neighbor
			for (Pixel2D neighbor : neighbors) {
				// Check if neighbor hasn't been visited yet (distance == -1) and is not an obstacle
				if (ans.getPixel(neighbor) == -1 && this.getPixel(neighbor) != obsColor) {
					// Set neighbor's distance to current distance + 1
					ans.setPixel(neighbor, ans.getPixel(current) + 1);
					// Add neighbor to queue for further exploration
					q.add(neighbor);
				}
			}
		}
		return ans;
	}

	////////////////////// Private Methods ///////////////////////

	/**
	 * Gets all valid neighbors of a given pixel (up, down, left, right).
	 * In non-cyclic mode, only returns neighbors within map boundaries.
	 * In cyclic mode, edges wrap around (e.g., right edge connects to left edge).
	 *
	 * @param p the pixel to get neighbors for
	 * @param cyclic if true, map is cyclic (edges wrap around), otherwise edges are boundaries
	 * @return an ArrayList of valid neighboring pixels
	 */
	private ArrayList<Pixel2D> getAllNeighbors(Pixel2D p, boolean cyclic) {
		ArrayList<Pixel2D> neighbors = new ArrayList<>();

		// Add the up neighbor (increasing Y coordinate)
		if (p.getY() < getHeight() - 1) {
			// Standard case: not at top edge
			neighbors.add(new Index2D(p.getX(), p.getY() + 1));
		}
		else if (cyclic) {
			// Cyclic case: wrap to bottom of map
			neighbors.add(new Index2D(p.getX(), 0));
		}

		// Add the down neighbor (decreasing Y coordinate)
		if (p.getY() > 0) {
			// Standard case: not at bottom edge
			neighbors.add(new Index2D(p.getX(), p.getY() - 1));
		}
		else if (cyclic) {
			// Cyclic case: wrap to top of map
			neighbors.add(new Index2D(p.getX(), getHeight() - 1));
		}

		// Add the right neighbor (increasing X coordinate)
		if (p.getX() < getWidth() - 1) {
			// Standard case: not at right edge
			neighbors.add(new Index2D(p.getX() + 1, p.getY()));
		}
		else if (cyclic) {
			// Cyclic case: wrap to left edge of map
			neighbors.add(new Index2D(0, p.getY()));
		}

		// Add the left neighbor (decreasing X coordinate)
		if (p.getX() > 0) {
			// Standard case: not at left edge
			neighbors.add(new Index2D(p.getX() - 1, p.getY()));
		}
		else if (cyclic) {
			// Cyclic case: wrap to right edge of map
			neighbors.add(new Index2D(getWidth() - 1, p.getY()));
		}

		return neighbors;
	}

	/**
	 * Recursively fills a connected component of pixels with the same color.
	 * This is the core of the flood fill algorithm.
	 *
	 * @param index the current pixel being processed
	 * @param originColor the original color that should be replaced
	 * @param new_v the new color to fill with
	 * @param cyclic if true, treats the map as cyclic (edges wrap around)
	 * @return the number of pixels filled by this recursive call and its children
	 */
	public int recursiveFill(Pixel2D index, int originColor, int new_v,  boolean cyclic) {
		// Base case: if current pixel doesn't have the origin color, stop recursion
		if (_map[index.getY()][index.getX()] != originColor) {
			return 0;
		}

		// Fill current pixel with new color
		_map[index.getY()][index.getX()] = new_v;
		int result = 1;  // Count this pixel

		// Recursively fill all neighbors that have the same origin color
		ArrayList<Pixel2D> neighbors = getAllNeighbors(index, cyclic);
		for (Pixel2D neighbor : neighbors) {
			// Add the count of pixels filled by recursive calls
			result += recursiveFill(neighbor, originColor, new_v, cyclic);
		}

		return result;
	}
}


/**
 * Helper class used for BFS pathfinding algorithm.
 * Stores a pixel, its parent in the search tree, and visited status.
 */
class Node {
	/** The pixel coordinate this node represents */
	public Pixel2D current;
	/** The parent node in the BFS tree (null for the root) */
	public Node parent;
	/** Whether this node has been visited during BFS */
	public boolean visited;

	/**
	 * Constructs a new Node with the given pixel.
	 * @param p the pixel coordinate for this node
	 */
	public Node(Pixel2D p) {
		this.current = p;
		this.parent = null;
		this.visited = false;
	}

	/**
	 * Marks this node as visited and sets its parent.
	 * Used during BFS to track the path back to the starting node.
	 * @param parent the parent node in the BFS tree
	 */
	public void visit(Node parent) {
		this.parent = parent;
		this.visited = true;
	}
}